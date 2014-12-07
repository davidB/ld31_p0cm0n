/// License [CC0](http://creativecommons.org/publicdomain/zero/1.0/)
package ld31;

import javafx.scene.Scene;

import javax.inject.Inject;
import javax.inject.Provider;

import jme3_ext.AppState0;
import jme3_ext.Hud;
import jme3_ext.HudTools;
import jme3_ext.InputMapper;
import jme3_ext.PageManager;
import jme3_ext_deferred.Helpers4Lights;
import lombok.RequiredArgsConstructor;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx_ext.Iterable4AddRemove;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Environment;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3x.jfx.FxPlatformExecutor;

@RequiredArgsConstructor(onConstructor=@__(@Inject))
public class PageInGame extends AppState0 {
	private final HudInGame hudController;
	private final HudTools hudTools;
	private final Commands controls;
	private final InputMapper inputMapper;
	private final Provider<PageManager> pm; // use Provider as Hack to break the dependency cycle PageManager -> Page -> PageManager
	private final AppStateDeferredRendering appStateDeferredRendering;
	private final AppStateDebug appStateDebug;
	private final PageEnd pageEnd;
	private final PageIntro pageIntro;

	private Hud<HudInGame> hud;

	Subscription inputSub;
	final Node scene = new Node("scene");
	final Control4Translation c4t = new Control4Translation();
	int spawnEventCnt = 0;
	final Tiles tiles = new Tiles();
	public int pelletTotal;
	public int pelletAte;
	final Spatial[] pellets = new Spatial[tiles.width * tiles.height];
	public final TimeCounter timeCount = new TimeCounter();
	public final float speedXMax = 12f;
	public final float speedZMax = 12f;
	float boostTimer = 0;
	public final long timeMax = 0;//(long)(((((float)tiles.width) / speedXMax) * tiles.height + (((float)tiles.height) / speedZMax)));

	@Override
	protected void doInitialize() {
		hud = hudTools.newHud("Interface/HudInGame.fxml", hudController);
		app.getStateManager().attach(appStateDeferredRendering);
		app.getStateManager().attach(appStateDebug);
		app.getStateManager().attach(pageIntro);
	}

	void reset() {
		doDisable();
		doEnable();
	}

	@Override
	protected void doEnable() {
		hudTools.show(hud);
		app.getInputManager().addRawInputListener(inputMapper.rawInputListener);
		FxPlatformExecutor.runOnFxApplication(() -> {
			HudInGame p = hud.controller;
			p.settings.onActionProperty().set((v) -> {
				app.enqueue(()-> {
					pm.get().goTo(Pages.Settings.ordinal());
					return true;
				});
			});
			p.quit.onActionProperty().set((v) -> {
				app.enqueue(()->{
					app.stop();
					return true;
				});
			});
		});
		//inputMapper.last.subscribe((v) -> {System.out.println("last evt : " + v + " .. " + hud.region.isFocused() + " .. " + Helpers4Javafx.findFocused(hud.region.getParent()));});
		inputSub = Subscriptions.from(
				controls.exit.value.subscribe((v) -> {
					if (!v) hud.controller.quit.fire();
				})
				, controls.moveX.value.subscribe((v) -> {c4t.speedX = v * speedXMax;})
				, controls.moveZ.value.subscribe((v) -> {c4t.speedZ = v * -speedZMax;})
				, controls.moveX.value.subscribe((v) -> {if (v != 0) timeCount.start();})
				, controls.moveZ.value.subscribe((v) -> {if (v != 0) timeCount.start();})
				);
		app.enqueue(() -> {
			setupCamera();
			spawnScene();
			activatePlayer(true);
			FxPlatformExecutor.runOnFxApplication(() -> {
				hud.controller.pelletCount.setText(String.format("%d", (pelletTotal - pelletAte)));
				hud.controller.timeCount.setText(String.format("%d", score()));

				//HACK TO force focus (keyboard) on play area
				//hud.region.focusedProperty().addListener((v) -> System.out.println("focus change : " + v));
				//hud.region.requestFocus();
				Scene scene = hud.region.getScene();
				//scene.getWindow().requestFocus();
				//Event.fireEvent(scene.getWindow(), new MouseEvent(MouseEvent.MOUSE_CLICKED, 10, 10, (int)scene.getWindow().getX() + 10, (int)scene.getWindow().getY() + 10, MouseButton.PRIMARY, 1, true, true, true, true, true, true, true, true, true, true, null));
				try {
					java.awt.Robot r = new java.awt.Robot();
//					r.mouseMove((int)scene.getWindow().getX() + 10, (int)scene.getWindow().getY() + 10);
					r.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
					r.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			});
			return true;
		});
		timeCount.reset();
	}



	@Override
	protected void doUpdate(float tpf) {
		super.doUpdate(tpf);
		if (timeCount.inc(tpf)) {
			FxPlatformExecutor.runOnFxApplication(() -> {
				hud.controller.timeCount.setText(String.format("%d", score()));
			});
		}
	}

	@Override
	protected void doDisable() {
		app.enqueue(() -> {
			unspawnScene();
			return true;
		});
		app.getInputManager().removeRawInputListener(inputMapper.rawInputListener);
		hudTools.hide(hud);
		if (inputSub != null){
			inputSub.unsubscribe();
			inputSub = null;
		}
	}

	@Override
	protected void doDispose() {
		// TODO Auto-generated method stub
		super.doDispose();
		app.getStateManager().detach(appStateDebug);
		app.getStateManager().detach(appStateDeferredRendering);
	}

	private void eatPellet(int x, int z) {
		Spatial s = PageInGame.this.pellets[x + z * tiles.width];
		if (s != null) {
			s.removeFromParent();
			appStateDeferredRendering.processor.lights.ar.remove.onNext((Geometry)((Node)s).getChild("light"));
			PageInGame.this.pellets[x + z * tiles.width] = null;
			if (s.getUserData("BOOST") == Boolean.TRUE) {
				boostTimer += 3.0f;
			} else {
				PageInGame.this.pelletAte += 1;
				FxPlatformExecutor.runOnFxApplication(() -> {
					hud.controller.pelletCount.setText(String.format("%d", (pelletTotal - pelletAte)));
				});
			}
		}
		if (pelletAte >= pelletTotal) end();
	}

	public long score() {
		return (timeMax - (long)Math.floor(timeCount.time));
	}

	private void end() {
		timeCount.stop();
		activatePlayer(false);
		app.getStateManager().attach(pageEnd);
	}

	void spawnScene() {
		scene.getChildren().clear();
		scene.attachChild(makePellets());
		scene.attachChild(makePlayer());
		scene.attachChild(makeEnvironment());
		app.getRootNode().attachChild(scene);
	}

	@SuppressWarnings("unchecked")
	void unspawnScene() {
		scene.removeFromParent();
		Iterable4AddRemove<Geometry> lights = appStateDeferredRendering.processor.lights;
		for(Geometry l : (Iterable<Geometry>)lights.data.clone()){lights.ar.remove.onNext(l);}
		c4t.setSpatial(null);
	}

	void setupCamera() {
		Camera cam = app.getCamera();
		Vector3f target = new Vector3f(tiles.width * 0.5f + 6f, 0f, tiles.height * 0.5f);
		float tan = cam.getFrustumTop() / cam.getFrustumNear(); //top = FastMath.tan(fovY * FastMath.DEG_TO_RAD * .5f) * near
		float marginZ = 2f;
		float y = ((tiles.width * 0.5f) + marginZ)  / tan;
		cam.setLocation(new Vector3f(0,y,0).addLocal(target));
		cam.lookAt(target, new Vector3f(0, 0, -1));

		app.getListener().setLocation(cam.getLocation());
		app.getListener().setRotation(cam.getRotation());
		app.getAudioRenderer().setEnvironment(new Environment(Environment.Garage));
	}
	Spatial makeEnvironment() {
		Node root = new Node("environment");
		root.attachChild(makeWalls());
		//root.setLocalTranslation(root.getLocalTranslation().add(0, 0, tiles.height * 0.5f));
		return root;
	}

	Spatial makeFloor(){
		ColorRGBA color = ColorRGBA.White;
		Geometry g = new Geometry("Player", new Quad(tiles.width, tiles.height));
		g.center();
		Material mat = new Material(app.getAssetManager(), "MatDefs/deferred/gbuffer.j3md");
		mat.setColor("Color", color);
		g.setMaterial(mat);
		return g;
	}

	Spatial makeWalls(){
		//BatchNode root = new BatchNode("walls");
		Node root = new Node("walls");
		Material mat = new Material(app.getAssetManager(), "MatDefs/deferred/gbuffer.j3md");
		//Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", ColorRGBA.Green);
		for(int x = 0 ; x < tiles.width; x++) {
			for(int z = 0 ; z < tiles.height; z++) {
				if (tiles.has(Tiles.EMPTY, x, z)){
					boolean top = tiles.has(Tiles.EMPTY, x, z - 1, Tiles.EMPTY)||!tiles.has(Tiles.PLAYER_ALLOWED, x, z - 1, Tiles.EMPTY);
					boolean bottom = tiles.has(Tiles.EMPTY, x, z + 1, Tiles.EMPTY)||!tiles.has(Tiles.PLAYER_ALLOWED, x, z + 1, Tiles.EMPTY);
					boolean right = tiles.has(Tiles.EMPTY, x + 1, z, Tiles.EMPTY)||!tiles.has(Tiles.PLAYER_ALLOWED, x + 1, z, Tiles.EMPTY);
					boolean left = tiles.has(Tiles.EMPTY, x - 1, z, Tiles.EMPTY)||!tiles.has(Tiles.PLAYER_ALLOWED, x - 1, z, Tiles.EMPTY);
					if (top && left) root.attachChild(makeBrick(x,z, mat));
					if (top && right) root.attachChild(makeBrick(x+0.5f,z, mat));
					if (bottom && left) root.attachChild(makeBrick(x, z+0.5f, mat));
					if (bottom && right) root.attachChild(makeBrick(x +0.5f, z + 0.5f, mat));
				}
			}
		}
		//root.batch();
		return root;
	}

	Spatial makeBrick(float x, float z, Material mat) {
		float margin = 0.01f;
		//TODO use noise (perlin,...) to generate level
		float y = 0.5f + FastMath.nextRandomFloat() * 2f;
		Geometry g = new Geometry("brick", new Box(new Vector3f(x+margin, 0, z+margin), new Vector3f(x+0.5f-margin, y, z + 0.5f-margin)));
		g.setMaterial(mat);
		return g;
	}

	Spatial makePlayer() {
		Node root = (Node) app.getRootNode().getChild("player");
		if (root == null) {
			root = new Node("player");

			ColorRGBA color = ColorRGBA.Yellow;
			//Geometry g = new Geometry("Player", new Sphere(16, 16, 0.7f));
			Geometry g = new Geometry("Player", new Box(0.7f, 0.7f, 0.7f));
			Material mat = new Material(app.getAssetManager(), "MatDefs/deferred/gbuffer.j3md");
			mat.setColor("Color", color);
			g.setMaterial(mat);
			root.attachChild(g);

			//Geometry light = Helpers4Lights.newSpotLight("playerLight", 10f, 50f, color, app.getAssetManager());
			Geometry light = Helpers4Lights.newPointLight("playerLight", 10f, color, app.getAssetManager());
			light.setLocalTranslation(0, 5f - 1f, 0);
			root.attachChild(light);
			//dsp.addLight(pointLight, true);
			appStateDeferredRendering.processor.lights.ar.add.onNext(light);
		}

		root.setLocalTranslation(14, 0, 23.5f);
		activatePlayer(false);
		return root;
	}

	void activatePlayer(boolean v) {
		Node root = (Node) app.getRootNode().getChild("player");
		if (root != null) {
			if (v) {
				root.addControl(c4t);
				root.addControl(new Control4EatPellet());
			} else {
				root.removeControl(c4t);
				root.removeControl(Control4EatPellet.class);
			}
		}
	}

	Spatial makePellets(){
		Node root = new Node("pellets");
		pelletTotal = 0;
		pelletAte = 0;
		for(int x = 0 ; x < tiles.width; x++) {
			for(int z = 0 ; z < tiles.height; z++) {
				int id = x + z * tiles.width;
				pellets[id] = null;
				//System.out.printf(">> %d, %d = %d / %s \n", x, z, tiles.tile(x, z), tiles.has(Tiles.PELLET, x, z));
				if (tiles.has(Tiles.PELLET, x, z)){
					Spatial pellet = makePellet();
					pellet.setName("p" + id);
					translateToTile(pellet, x, z, 0.4f);
					root.attachChild(pellet);
					pellets[id] = pellet;
					pelletTotal++;
				} else if (tiles.has(Tiles.BOOST, x, z)) {
					Spatial pellet = makePelletBoost();
					pellet.setName("p" + id);
					translateToTile(pellet, x, z, 0.4f);
					root.attachChild(pellet);
					pellets[id] = pellet;
				}
			}
		}
		return root;
	}

	Spatial makePellet() {
		Node n = new Node("pellet");
		ColorRGBA color = ColorRGBA.White;
		AssetManager assetManager = app.getAssetManager();
		float size = 0.4f;
		Geometry geom = new Geometry("particle", new Quad(size, size));
		geom.setLocalTranslation(-0.5f * size, 0.5f * -size, 0.0f);
		Material lightMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		lightMaterial.setColor("Color", color);
		lightMaterial.setTexture("LightMap", assetManager.loadTexture("Textures/pellet.jpg"));
		lightMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);
		lightMaterial.getAdditionalRenderState().setDepthWrite(false);
		geom.setMaterial(lightMaterial);
		geom.setQueueBucket(Bucket.Transparent);
		BillboardControl billboarder = new BillboardControl();
		//billboarder.setAlignment(BillboardControl.Alignment.Camera);
		//		geom.addControl(billboarder);
		//		n.attachChild(geom);
		Node anchor0 = new Node();
		anchor0.addControl(billboarder);
		anchor0.attachChild(geom);
		n.attachChild(anchor0);

		//pointLight.setColor(ColorRGBA.randomColor().multLocal(0.1f));
		Geometry pointLight = Helpers4Lights.newPointLight("light", 2f, color, assetManager);
		pointLight.center();
		n.attachChild(pointLight);
		//dsp.addLight(pointLight, true);
		appStateDeferredRendering.processor.lights.ar.add.onNext(pointLight);
		n.center();
		return n;
	}

	Spatial makePelletBoost() {
		Node n = new Node("pellet");
		n.setUserData("BOOST", true);
		ColorRGBA color = ColorRGBA.Orange;
		AssetManager assetManager = app.getAssetManager();
		float size = 0.8f;
		Geometry geom = new Geometry("particle", new Quad(size, size));
		geom.setLocalTranslation(-0.5f * size, 0.5f * -size, 0.0f);
		Material lightMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		lightMaterial.setColor("Color", color);
		lightMaterial.setTexture("LightMap", assetManager.loadTexture("Textures/pellet.jpg"));
		lightMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);
		lightMaterial.getAdditionalRenderState().setDepthWrite(false);
		geom.setMaterial(lightMaterial);
		geom.setQueueBucket(Bucket.Transparent);
		BillboardControl billboarder = new BillboardControl();
		//billboarder.setAlignment(BillboardControl.Alignment.Camera);
		//		geom.addControl(billboarder);
		//		n.attachChild(geom);
		Node anchor0 = new Node();
		anchor0.addControl(billboarder);
		anchor0.attachChild(geom);
		n.attachChild(anchor0);

		//pointLight.setColor(ColorRGBA.randomColor().multLocal(0.1f));
		Geometry pointLight = Helpers4Lights.newPointLight("light", 2f, color, assetManager);
		pointLight.center();
		n.attachChild(pointLight);
		//dsp.addLight(pointLight, true);
		appStateDeferredRendering.processor.lights.ar.add.onNext(pointLight);
		n.center();
		return n;

	}

	protected void addInfo(String info) {
		FxPlatformExecutor.runOnFxApplication(() -> {
			// autoscroll to bottom
			hudController.consoleLog.appendText("\n"+info);
			hudController.consoleLog.setScrollTop(Double.MIN_VALUE);
		});
	}

	void fromTile(Vector3f store, int x, int y) {
		store.set(x + 0.5f, 0, y + 0.5f);
	}

	void translateToTile(Spatial store, int x, int z, float y) {
		store.setLocalTranslation(x + 0.5f, y, z + 0.5f);
	}

	class Control4Translation extends AbstractControl {
		public float speedX = 0f;
		public float speedZ = 0f;
		private Vector3f v3 = new Vector3f();
		@Override
		protected void controlUpdate(float tpf) {
			v3.set(getSpatial().getLocalTranslation());
			// loop from top to bottom, left to right
			float boost = 1;
			if (boostTimer > 0) {
				boostTimer = (float)Math.max(0, boostTimer - tpf);
				boost = 2;
				ColorRGBA c = (boostTimer > 0) ? ColorRGBA.Orange : ColorRGBA.Yellow;
				((Geometry)((Node)spatial).getChild(0)).getMaterial().setColor("Color", c);
			}
			float nx = ((float)tiles.width + v3.x + boost * speedX * tpf) % ((float)tiles.width);
			float nz = ((float)tiles.height + v3.z + boost * speedZ * tpf) % ((float)tiles.height);
			if (PageInGame.this.tiles.has(Tiles.PLAYER_ALLOWED, (int)Math.floor(nx+Math.signum(speedX) * 0.5), (int)Math.floor(nz+Math.signum(speedZ) * 0.5))) {
				getSpatial().setLocalTranslation(nx, v3.y, nz);
			} else if (speedX != 0 && PageInGame.this.tiles.has(Tiles.PLAYER_ALLOWED, (int)Math.floor(nx+Math.signum(speedX) * 0.5), (int)Math.floor(v3.z))) {
				getSpatial().setLocalTranslation(nx, v3.y, v3.z);
			} else if (speedZ != 0 && PageInGame.this.tiles.has(Tiles.PLAYER_ALLOWED, (int)Math.floor(v3.x), (int)Math.floor(nz+Math.signum(speedZ) * 0.5))) {
				getSpatial().setLocalTranslation(v3.x, v3.y, nz);
			}
		}

		@Override
		protected void controlRender(RenderManager rm, ViewPort vp) {
		}

	}

	class Control4EatPellet extends AbstractControl {
		final AudioNode audioBoost;
		final AudioNode audioPellet;
		final Vector3f v3 = new Vector3f();

		public Control4EatPellet() {
			audioPellet = new AudioNode(app.getAssetManager(), "Sounds/pellet.wav", false); // buffered
			audioPellet.setLooping(false);
			audioPellet.setPositional(true);
			audioBoost = new AudioNode(app.getAssetManager(), "Sounds/boost.wav", false); // buffered
			audioBoost.setLooping(false);
			audioBoost.setPositional(true);
		}
		public void setSpatial(Spatial spatial) {
			super.setSpatial(spatial);
			if (spatial == null) {
				audioPellet.removeFromParent();
				audioBoost.removeFromParent();
			} else {
				Node root = (Node)spatial;
				root.attachChild(audioPellet);
				root.attachChild(audioBoost);
			}
		}

		@Override
		protected void controlUpdate(float tpf) {
			v3.set(getSpatial().getLocalTranslation());
			int x = (int)Math.floor(v3.x);
			int z = (int)Math.floor(v3.z);
			Spatial s = PageInGame.this.pellets[x + z * tiles.width];
			//if (PageInGame.this.tiles.has(Tiles.PELLET, x, z)) {PageInGame.this.tiles.has(Tiles.BOOST, x, z)) {
			if (s != null) {
				eatPellet(x,z);
				if (s.getUserData("BOOST") == Boolean.TRUE){
					audioBoost.playInstance();
				} else {
					audioPellet.playInstance();
				}
			}
		}

		@Override
		protected void controlRender(RenderManager rm, ViewPort vp) {
		}
	}
}



