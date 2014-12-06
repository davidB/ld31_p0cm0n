/// License [CC0](http://creativecommons.org/publicdomain/zero/1.0/)
package ld31;

import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;

import javax.inject.Inject;
import javax.inject.Provider;

import jme3_ext.AppState0;
import jme3_ext.Hud;
import jme3_ext.HudTools;
import jme3_ext.InputMapper;
import jme3_ext.InputMapperHelpers;
import jme3_ext.InputTextureFinder;
import jme3_ext.PageManager;
import jme3_ext_deferred.Helpers4Lights;
import lombok.RequiredArgsConstructor;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx_ext.Iterable4AddRemove;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.input.event.InputEvent;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3x.jfx.FxPlatformExecutor;

@RequiredArgsConstructor(onConstructor=@__(@Inject))
public class PageInGame extends AppState0 {
	private final HudInGame hudController;
	private final HudTools hudTools;
	private final Commands controls;
	private final InputMapper inputMapper;
	private final Provider<PageManager> pm;
	private final InputTextureFinder inputTextureFinders;
	private final AppStateDeferredRendering appStateDeferredRendering;
	private final AppStateDebug appStateDebug;

	private Hud<HudInGame> hud;

	Subscription inputSub;
	final Node scene = new Node("scene");
	final Control4Translation c4t = new Control4Translation();
	int spawnEventCnt = 0;
	final Tiles tiles = new Tiles();

	@Override
	protected void doInitialize() {
		hud = hudTools.newHud("Interface/HudInGame.fxml", hudController);
	}

	@Override
	protected void doEnable() {
		app.getStateManager().attach(appStateDeferredRendering);
		app.getStateManager().attach(appStateDebug);
		hudTools.show(hud);
		app.getInputManager().addRawInputListener(inputMapper.rawInputListener);

		inputSub = Subscriptions.from(
			controls.exit.value.subscribe((v) -> {
				if (!v) pm.get().goTo(Pages.Welcome.ordinal());
			})
			, inputMapper.last.subscribe((v) -> spawnEvent(v))
			, controls.action1.value.subscribe((v) -> action1(v))
			, controls.action2.value.subscribe((v) -> action2(v))
			, controls.action3.value.subscribe((v) -> action3(v))
			, controls.action4.value.subscribe((v) -> action4(v))
			, controls.moveX.value.subscribe((v) -> {c4t.speedX = v * 2f;})
			, controls.moveZ.value.subscribe((v) -> {c4t.speedZ = v * -2f;})
		);
		setupCamera();
		spawnScene();
	}

	@Override
	protected void doDisable() {
		unspawnScene();
		app.getInputManager().removeRawInputListener(inputMapper.rawInputListener);
		hudTools.hide(hud);
		if (inputSub != null){
			inputSub.unsubscribe();
			inputSub = null;
		}
		app.getStateManager().detach(appStateDebug);
		app.getStateManager().detach(appStateDeferredRendering);
	}

	private void action1(Boolean v) {
		action(v, hud.controller.action1);
	}

	private void action2(Boolean v) {
		action(v, hud.controller.action2);
	}

	private void action3(Boolean v) {
		action(v, hud.controller.action3);
	}

	private void action4(Boolean v) {
		action(v, hud.controller.action4);
	}

	private void action(Boolean v, javafx.scene.Node n) {
		FxPlatformExecutor.runOnFxApplication(() -> {
			Effect e = (v) ? new Glow(0.8): null;
			n.setEffect(e);
		});
	}

	void spawnScene() {
		app.enqueue(()-> {
			scene.getChildren().clear();
			scene.attachChild(makePellets());
			scene.attachChild(makePlayer());
			//scene.attachChild(makeEnvironment());
			app.getRootNode().attachChild(scene);
			return true;
		});
	}

	void unspawnScene() {
		app.enqueue(()-> {
			scene.removeFromParent();
			Iterable4AddRemove<Geometry> lights = appStateDeferredRendering.processor.lights;
			for(Geometry l : (Iterable<Geometry>)lights.data.clone()){lights.ar.remove.onNext(l);}
			c4t.setSpatial(null);
			return true;
		});
	}

	void spawnEvent(InputEvent evt) {
		addInfo(InputMapperHelpers.toString(evt, false));
		Quad q = new Quad(0.5f, 0.5f);
		Geometry g = new Geometry("Quad", q);
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		//mat.setColor("Color", ColorRGBA.Blue);
		String path = inputTextureFinders.findPath(evt);
		mat.setTexture("ColorMap", app.getAssetManager().loadTexture(path));
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		g.setQueueBucket(Bucket.Transparent);
		g.setMaterial(mat);

		BillboardControl billboard = new BillboardControl();
		g.addControl(billboard);

		spawnEventCnt++;
//		Animation anim = new Animation("goUp", 6.0f);
//		anim.addTrack(new TranslationTrack(new Vector3f((spawnEventCnt % 10) - 5f,10f,0f), 5.0f));
//		anim.addTrack(new RemoveTrack(5.0f));
//		AnimControl ac = new AnimControl();
//		ac.addAnim(anim);
//		g.addControl(ac);

//		g.setLocalTranslation(scene.getChild("player").getWorldTranslation());
//		app.enqueue(()-> {
//			scene.attachChild(g);
//			AnimChannel c = ac.createChannel();
//			c.setLoopMode(LoopMode.DontLoop);
//			c.setAnim("goUp");
//			return true;
//		});
	}
	void setupCamera() {
		Camera cam = app.getCamera();
		Vector3f target = new Vector3f(tiles.width * 0.5f, 0f, tiles.height * 0.5f);
		float tan = cam.getFrustumTop() / cam.getFrustumNear(); //top = FastMath.tan(fovY * FastMath.DEG_TO_RAD * .5f) * near
		float marginZ = 2f;
		float y = ((tiles.width * 0.5f) + marginZ)  / tan;
		cam.setLocation(new Vector3f(0,y,0).addLocal(target));
		cam.lookAt(target, new Vector3f(0, 0, -1));
	}
	Spatial makePlayer() {
		Node root = new Node("player");
		Geometry g = new Geometry("Player", new Sphere(16, 16, 0.5f));
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", ColorRGBA.Red);
		g.setMaterial(mat);
		root.attachChild(g);

		root.addControl(c4t);
		return root;
	}

	Spatial makePellets(){
		Node root = new Node("pellets");
		for(int x = 0 ; x < tiles.width; x++) {
			for(int z = 0 ; z < tiles.height; z++) {
				//System.out.printf(">> %d, %d = %d / %s \n", x, z, tiles.tile(x, z), tiles.has(Tiles.PELLET, x, z));
				if (tiles.has(Tiles.PELLET, x, z)){
					Spatial pellet = makePellet();
					pellet.setLocalTranslation(x + 0.5f, 0, z + 0.5f);
					root.attachChild(pellet);
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
		Geometry pointLight = Helpers4Lights.newPointLight("light", 1.2f, color, assetManager);
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

	class Control4Translation extends AbstractControl {
		public float speedX = 0f;
		public float speedZ = 0f;
		@Override
		protected void controlUpdate(float tpf) {
			Vector3f pos = getSpatial().getLocalTranslation();
			pos.x += speedX * tpf;
			pos.z += speedZ * tpf;
			getSpatial().setLocalTranslation(pos);
		}

		@Override
		protected void controlRender(RenderManager rm, ViewPort vp) {
		}

	}
}


class Tiles {
	final static int tiles_pacmanclassic_W = 28;
	final static int tiles_pacmanclassic_H = 31;
	final static int[] tiles_pacmanclassic = {
		00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,
		00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,
		00,	07,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	07,	00,
		00,	11,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	11,	00,
		00,	07,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	07,	00,
		00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,
		00,	07,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	07,	00,
		00,	07,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	07,	00,
		00,	07,	07,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	07,	07,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	01,	01,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	01,	01,	01,	01,	01,	01,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,	01,	01,	01,	01,	01,	01,	00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	01,	01,	01,	01,	01,	01,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,
		00,	07,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	07,	00,
		00,	07,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	07,	00,
		00,	11,	07,	07,	00,	00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,	00,	07,	07,	11,	00,
		00,	00,	00,	07,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	07,	00,	00,	00,
		00,	00,	00,	07,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	07,	00,	00,	00,
		00,	07,	07,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	07,	07,	00,
		00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,
		00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,
		00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,
		00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,
	};

	public final static int EMPTY			= 0x00;
	public final static int GHOST_ALLOWED	= 0x01;
	public final static int PLAYER_ALLOWED	= 0x02;
	public final static int PELLET			= 0x04;
	public final static int ENERGIZER		= 0x08;

	public final int width = tiles_pacmanclassic_W;
	public final int height = tiles_pacmanclassic_H;
	private final int[] tiles = new int[width * height];

	public Tiles() {
		reset();
	}

	public void reset() {
		System.arraycopy(tiles_pacmanclassic, 0, tiles, 0, tiles.length);
	}

	public int tile(int x, int y) {
		return tiles[x + y * width];
	}

	public boolean has(int mask, int x, int y) {
		int v = tiles[x + y * width];
		return (v == mask) || ((v & mask) != 0); // first test to match EMPTY mask
	}

	public int removePellet(int x, int y) {
		int v = tiles[x + y * width] & ~PELLET;
		tiles[x + y * width] = v;
		return v;
	}

	public int removeEnergizer(int x, int y) {
		int v = tiles[x + y * width] & ~ENERGIZER;
		tiles[x + y * width] = v;
		return v;
	}
}