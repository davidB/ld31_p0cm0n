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
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.BatchNode;
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
	private final static ColorRGBA[] ghostColors = new ColorRGBA[]{ColorRGBA.Pink, ColorRGBA.Red, ColorRGBA.Cyan, ColorRGBA.Brown};

	private Hud<HudInGame> hud;

	Subscription inputSub;
	final Node scene = new Node("scene");
	final Control4Translation c4t = new Control4Translation();
	int spawnEventCnt = 0;
	final Tiles tiles = new Tiles();
	public int pelletTotal;
	public int pelletAte;
	final Spatial[] pellets = new Spatial[tiles.width * tiles.height];
	final Spatial[] ghosts = new Spatial[4];
	final Spatial[] players = new Spatial[1];
	public final TimeCounter timeCount = new TimeCounter();
	public final float speedMax = 12f;
	float boostTimer = 0;
	public final long timeMax = 0;//(long)(((((float)tiles.width) / speedXMax) * tiles.height + (((float)tiles.height) / speedZMax)));

	@Override
	protected void doInitialize() {
		hud = hudTools.newHud("Interface/HudInGame.fxml", hudController);
		app.getStateManager().attach(pageIntro);
		app.getStateManager().attach(appStateDeferredRendering);
		app.getStateManager().attach(appStateDebug);
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
				, controls.moveX.value.subscribe((v) -> {c4t.speedX = v * speedMax;})
				, controls.moveZ.value.subscribe((v) -> {c4t.speedZ = v * -speedMax;})
				, controls.moveX.value.subscribe((v) -> {if (v != 0) timeCount.start();})
				, controls.moveZ.value.subscribe((v) -> {if (v != 0) timeCount.start();})
				);
	}



	@Override
	protected void doUpdate(float tpf) {
		super.doUpdate(tpf);
		if (timeCount.inc(tpf)) {
			FxPlatformExecutor.runOnFxApplication(() -> {
				hud.controller.timeCount.setText(String.format("%d", score()));
			});
		}
		// brut force collision
		for(Spatial p : players) {
			for(Spatial g : ghosts) {
				if (p == null || g == null) continue;
				if (g.getWorldTranslation().distance(p.getWorldTranslation()) <= 0.6f) {
					end(false);
					break;
				}
 			}
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
		if (pelletAte >= pelletTotal) end(true);
		if (pelletAte == 40) spawnGhost(0);
		else if (pelletAte == 80) spawnGhost(1);
		else if (pelletAte == 130) spawnGhost(2);
		else if (pelletAte == 200) spawnGhost(3);
	}

	public long score() {
		return (timeMax - (long)Math.floor(timeCount.time));
	}

	public void start() {
		timeCount.reset();
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
	}

	private void end(boolean success) {
		timeCount.stop();
		activatePlayer(false);
		pageEnd.success = success;
		app.getStateManager().attach(pageEnd);
	}

	void spawnScene() {
		scene.getChildren().clear();
		scene.attachChild(makePellets());
		scene.attachChild(makePlayer());
		scene.attachChild(makeEnvironment());
		scene.attachChild(makeGhosts());
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
		//root.attachChild(makeFloor());
		return root;
	}

	Spatial makeFloor(){
		ColorRGBA color = ColorRGBA.White;
		Geometry g = new Geometry("Player", new Quad(tiles.width, tiles.height));
		g.center();
		Material mat = new Material(app.getAssetManager(), "MatDefs/deferred/gbuffer.j3md");
		mat.setColor("Color", color);
		g.setMaterial(mat);
		//root.setLocalTranslation(root.getLocalTranslation().add(0, 0, tiles.height * 0.5f));
		return g;
	}

	Spatial makeWalls(){
		BatchNode root = new BatchNode("walls");
		//Node root = new Node("walls");
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
		root.batch();
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
		players[0] = root;
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
				scene.detachChildNamed("ghosts");
				root.removeControl(c4t);
				root.removeControl(Control4EatPellet.class);
			}
		}
	}

	Spatial makeGhosts() {
		Node root = (Node) app.getRootNode().getChild("ghosts");
		if (root == null) {
			root = new Node("ghosts");
		}
		root.detachAllChildren();
		for (int i = 0; i < ghosts.length; i++) ghosts[i] = null;
		return root;
	}

	Spatial spawnGhost(int i) {
		Node root = (Node) app.getRootNode().getChild("ghosts");
		Spatial g = null;
		if (root != null) {
			g = makeGhost(ghostColors[i%ghostColors.length]);
			g.setLocalTranslation(14, 0, 11.5f);
			ghosts[i] = g;
			root.attachChild(g);
			g.addControl(new Control4Ghost(i));
		}
		return g;
	}

	Spatial makeGhost(ColorRGBA c) {
		ParticleEmitter g = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 15);
		Material mat_red = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");
		mat_red.setTexture("Texture", app.getAssetManager().loadTexture("Effects/boost.png"));
		g.setMaterial(mat_red);
		g.setImagesX(2);
		g.setImagesY(2); // 2x2 texture animation
		g.setEndColor(ColorRGBA.LightGray);
		g.setStartColor(c);
		g.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
		g.setStartSize(1.3f);
		g.setEndSize(0.1f);
		g.setGravity(0, 0, 0);
		g.setLowLife(0.5f);
		g.setHighLife(1.5f);
		g.getParticleInfluencer().setVelocityVariation(0.3f);
		g.center();
		return g;
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
		//Geometry pointLight = Helpers4Lights.newPointLight("light", 0.8f+FastMath.nextRandomFloat() * 1.2f, color, assetManager);
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
		Geometry pointLight = Helpers4Lights.newPointLight("light", 2.2f, color, assetManager);
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
		//ParticleEmitter boostE;
		boolean boosting = false;

		@Override
		public void setSpatial(Spatial spatial) {
			super.setSpatial(spatial);
//			if (spatial != null) {
//				boostE = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
//				Material mat_red = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");
//				mat_red.setTexture("Texture", app.getAssetManager().loadTexture("Effects/boost.png"));
//				boostE.setMaterial(mat_red);
//				boostE.setImagesX(2);
//				boostE.setImagesY(2); // 2x2 texture animation
//				boostE.setEndColor(ColorRGBA.Yellow);
//				boostE.setStartColor(ColorRGBA.Orange);
//				boostE.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
//				boostE.setStartSize(1.3f);
//				boostE.setEndSize(0.1f);
//				boostE.setGravity(0, 0, 0);
//				boostE.setLowLife(1f);
//				boostE.setHighLife(3f);
//				boostE.getParticleInfluencer().setVelocityVariation(0.3f);
//				((Node)spatial).attachChild(boostE);
//			} else {
//				boostE.removeFromParent();
//				boostE = null;
//			}
		}
		@Override
		protected void controlUpdate(float tpf) {
			v3.set(getSpatial().getLocalTranslation());
			// loop from top to bottom, left to right
			float boost = 1;
			if (boostTimer > 0) {
				boostTimer = (float)Math.max(0, boostTimer - tpf);
				boost = 2;
			}
			if (boostTimer > 0 && ! boosting) {
				boosting = true;
				((Geometry)((Node)spatial).getChild(0)).getMaterial().setColor("Color", ColorRGBA.Orange);
				//((Node)spatial).attachChild(boostE);
				//boostE.setEnabled(true);
			} else if (boostTimer <= 0 && boosting) {
				boosting = false;
				((Geometry)((Node)spatial).getChild(0)).getMaterial().setColor("Color", ColorRGBA.Yellow);
				//boostE.setEnabled(false);
				//((Node)spatial).detachChild(boostE);
			}
//			if (speedX == 0) v3.x = (float)Math.floor(v3.x) + 0.5f;
//			if (speedZ == 0) v3.z = (float)Math.floor(v3.z) + 0.5f;
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
		final AudioNode audioSpeedMode;
		final AudioNode audioPellet;
		final Vector3f v3 = new Vector3f();

		public Control4EatPellet() {
			audioPellet = new AudioNode(app.getAssetManager(), "Sounds/pellet.wav", false); // buffered
			audioPellet.setLooping(false);
			audioPellet.setPositional(true);
			audioPellet.setVolume(0.4f);
			audioSpeedMode = new AudioNode(app.getAssetManager(), "Sounds/speed_mode.ogg", false); // buffered
			audioSpeedMode.setLooping(false);
			audioSpeedMode.setPositional(true);
		}
		public void setSpatial(Spatial spatial) {
			super.setSpatial(spatial);
			if (spatial == null) {
				audioPellet.removeFromParent();
				audioSpeedMode.removeFromParent();
			} else {
				Node root = (Node)spatial;
				root.attachChild(audioPellet);
				root.attachChild(audioSpeedMode);
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
					audioSpeedMode.play();
				} else {
					audioPellet.playInstance();
				}
			}
		}

		@Override
		protected void controlRender(RenderManager rm, ViewPort vp) {
		}
	}

	class Control4Ghost extends AbstractControl {
		final float speed = PageInGame.this.speedMax * 0.5f;
		final Vector3f[] directions = new Vector3f[]{ new Vector3f(-1,0,0), new Vector3f(0,0,-1), new Vector3f(1,0,0), new Vector3f(0,0,1)};
		int directionId = 0;
		final Vector3f v3 = new Vector3f();
		final AudioNode audioSpawn;
		boolean chaseMode = false;
		float timeoutChangeMod = 2;
		int changeModCount = 0;
		final int id;
		final Vector3f[] scatterTargets = new Vector3f[]{new Vector3f(2, 0, -2), new Vector3f(tiles.width - 2, 0, tiles.height + 2), new Vector3f(2, 0, tiles.height + 2), new Vector3f(tiles.width - 2, 0, -2)};

		Control4Ghost(int id) {
			this.id = id;
			audioSpawn = new AudioNode(app.getAssetManager(), "Sounds/boost.wav", false); // buffered
			audioSpawn.setLooping(false);
			audioSpawn.setPositional(true);
		}

		public void setSpatial(Spatial spatial) {
			super.setSpatial(spatial);
			if (spatial != null) {
				app.getRootNode().attachChild(audioSpawn);
				audioSpawn.play();
			} else {
				audioSpawn.removeFromParent();
			}
		};

		@Override
		protected void controlUpdate(float tpf) {
			updateMode(tpf);
			Vector3f pos = spatial.getLocalTranslation();
			int x = (int)Math.floor(pos.x);
			int z = (int)Math.floor(pos.z);
			if (checkNext(tpf, directions[directionId], v3)) {
				getSpatial().setLocalTranslation(v3);
				int nx = (int)Math.floor(v3.x);
				int nz = (int)Math.floor(v3.z);
				if (nx != x || nz != z) {
					directionId = nextDirection(nx, nz, 0);
				}
			} else {
				directionId = nextDirection(x, z, 1);
			}
		}

		protected void updateMode(float tpf) {
			timeoutChangeMod -= tpf;
			if (timeoutChangeMod <= 0) {
				changeModCount++;
				chaseMode = !chaseMode;
				//timeoutChangeMod = (chaseMode)?20:7;
				timeoutChangeMod = (chaseMode)?15:Math.max(2, 6 - changeModCount);
			}
		}

		Vector3f findTarget() {
			return (chaseMode)? players[0].getWorldTranslation() : scatterTargets[id % scatterTargets.length];
		}

		int nextDirection(int x, int z, int offset) {
			// entering in new title
			Vector3f target = findTarget();
			float d = Float.MAX_VALUE;
			int ni = directionId;
			for(int i = offset; i < 4; i++) {
				if (i == 2) continue; // opposite direction are forbidden
				int ti = (directionId + i) % directions.length;
				float td = distanceOfPlayer0(directions[ti], x, z, target);
				if (d > td) {
					d = td;
					ni = ti;
				}
			}
			return ni;
		}

		boolean checkNext(float tpf, Vector3f direction, Vector3f store ) {
			v3.set(direction).multLocal(speed * tpf).addLocal(spatial.getLocalTranslation());
			return PageInGame.this.tiles.has(Tiles.GHOST_ALLOWED, (int)Math.floor(v3.x+ direction.x * 0.5), (int)Math.floor(v3.z+ direction.z * 0.5));
		}

		float distanceOfPlayer0(Vector3f direction, int x, int z, Vector3f target) {
			float d = Float.MAX_VALUE;
			if (PageInGame.this.tiles.has(Tiles.GHOST_ALLOWED, (int)( x + direction.x), (int)( z + direction.z))) {
				d = v3.set(x + direction.x + 0.5f, 0, z + direction.z + 0.5f).distance(target);
			}
			return d;
		}

		@Override
		protected void controlRender(RenderManager rm, ViewPort vp) {
		}

	}
}



