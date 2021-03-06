/// License [CC0](http://creativecommons.org/publicdomain/zero/1.0/)
package ld31;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;

import javax.inject.Singleton;

import jme3_ext.AppSettingsLoader;
import jme3_ext.InputMapper;
import jme3_ext.InputMapperHelpers;
import jme3_ext.JmeModule;
import jme3_ext.PageManager;
import jme3_ext_deferred.MatIdManager;
import jme3_ext_deferred.MaterialConverter;
import jme3_ext_deferred.SceneProcessor4Deferred;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.asset.AssetManager;
import com.jme3.input.KeyInput;
import com.jme3.renderer.lwjgl.LwjglDisplayCustom;
import com.jme3.system.AppSettings;
import com.jme3x.jfx.FxPlatformExecutor;

import dagger.Module;
import dagger.Provides;

@Module(library=true, complete=false)
class DeferredModule {

	@Singleton
	@Provides
	public MatIdManager matIdManager() {
		return new MatIdManager();
	}

	@Singleton
	@Provides
	public SceneProcessor4Deferred sceneProcessor4Deferred(AssetManager a, MatIdManager m) {
		return new SceneProcessor4Deferred(a, m);
	}

	@Singleton
	@Provides
	public MaterialConverter mc(AssetManager a, MatIdManager m) {
		return new MaterialConverter(a,m);
	}
}

@Module(
	injects = {
		Main.class,
	},
	includes = {
		JmeModule.class,
		DeferredModule.class,
	}
)
class MainModule {

	@Provides
	public AppSettingsLoader appSettingsLoader() {
		return new AppSettingsLoader() {
			final String prefKey = "sandbox.MyGame";

			@Override
			public AppSettings loadInto(AppSettings settings) throws Exception{
				settings.load(prefKey);
				return settings;
			}

			@Override
			public AppSettings save(AppSettings settings) throws Exception{
				settings.save(prefKey);
				return settings;
			}
		};
	}

	@Singleton
	@Provides
	public SimpleApplication simpleApplication(AppSettings appSettings) {
		//HACK
		final CountDownLatch initializedSignal = new CountDownLatch(1);
		SimpleApplication app = new SimpleApplication(){
			@Override
			public void simpleInitApp() {
				flyCam.setEnabled(false);
				stateManager.detach(stateManager.getState(FlyCamAppState.class));
				initializedSignal.countDown();
			}

			@Override
			public void destroy() {
				super.destroy();
				FxPlatformExecutor.runOnFxApplication(() -> {
					Platform.exit();
				});
			}
		};
		app.setSettings(appSettings);
		app.setShowSettings(false);
		app.setDisplayStatView(false);
		app.setDisplayFps(false);
		app.start();
		try {
			initializedSignal.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return app;
	}

	@Singleton
	@Provides
	public AppSettings appSettings(AppSettingsLoader appSettingsLoader, ResourceBundle resources) {
		AppSettings settings = new AppSettings(true);
		try {
			settings = appSettingsLoader.loadInto(settings);
		} catch (Exception e) {
			e.printStackTrace();
		}
		settings.setTitle(resources.getString("title"));
		settings.setUseJoysticks(false);
		//settings.setGammaCorrection(true); //TODO jme 3.1.0
		settings.setResolution(1280, 720);
		settings.setVSync(false);
		settings.setFullscreen(false);
		settings.setDepthBits(24);
		settings.setCustomRenderer(LwjglDisplayCustom.class);
		return settings;
	}

	@Singleton
	@Provides
	public PageManager pageManager(SimpleApplication app, PageSettings pageSettings, PageInGame pageInGame) {
		AppState[] pages = new AppState[Pages.values().length];
		/*
         pages[Page.About.ordinal()] = new PageAbout(screen);
         pages[Page.InGame.ordinal()] = new PageInGame(screen);
         pages[Page.LevelSelection.ordinal()] = new PageLevelSelection(screen);
         pages[Page.Loading.ordinal()] = new PageLoading(screen);
         pages[Page.Result.ordinal()] = new PageResult(screen);
         pages[Page.Scores.ordinal()] = new PageScores(screen);
         pages[Page.Settings.ordinal()] = new PageSettings(screen);
		 */
		//pages[Pages.Welcome.ordinal()] = pageWelcome;
		pages[Pages.InGame.ordinal()] = pageInGame;
		pages[Pages.Settings.ordinal()] = pageSettings;
		PageManager pageManager = new PageManager(app.getStateManager(), pages);
		return pageManager;
	}

	@Singleton
	@Provides
	public Locale locale() {
		return Locale.getDefault();
	}

	@Provides
	public ResourceBundle resources(Locale locale) {
		return ResourceBundle.getBundle("Interface.labels", locale);
	}

	@Provides
	public FXMLLoader fxmlLoader(ResourceBundle resources) {
		FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setResources(resources);
		fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
		return fxmlLoader;
	}

	@Provides
	@Singleton
	public InputMapper inputMapper(Commands controls) {
		//TODO save / restore mapper, until then harcoded mapping
		InputMapper m = new InputMapper();
		InputMapperHelpers.mapKey(m, KeyInput.KEY_ESCAPE, controls.exit.value);
		InputMapperHelpers.mapKey(m, KeyInput.KEY_RETURN, controls.def.value);
		// arrow
		InputMapperHelpers.mapKey(m, KeyInput.KEY_UP, controls.moveZ.value, true);
		InputMapperHelpers.mapKey(m, KeyInput.KEY_DOWN, controls.moveZN.value, false);
		InputMapperHelpers.mapKey(m, KeyInput.KEY_RIGHT, controls.moveX.value, true);
		InputMapperHelpers.mapKey(m, KeyInput.KEY_LEFT, controls.moveXN.value, false);
		// WASD / ZQSD
		if (InputMapperHelpers.isKeyboardAzerty()) {
			InputMapperHelpers.mapKey(m, KeyInput.KEY_Z, controls.moveZ.value, true);
			InputMapperHelpers.mapKey(m, KeyInput.KEY_S, controls.moveZN.value, false);
			InputMapperHelpers.mapKey(m, KeyInput.KEY_Q, controls.moveX.value, false);
			InputMapperHelpers.mapKey(m, KeyInput.KEY_D, controls.moveXN.value, true);
		} else {
			InputMapperHelpers.mapKey(m, KeyInput.KEY_W, controls.moveZ.value, true);
			InputMapperHelpers.mapKey(m, KeyInput.KEY_S, controls.moveZN.value, false);
			InputMapperHelpers.mapKey(m, KeyInput.KEY_A, controls.moveX.value, false);
			InputMapperHelpers.mapKey(m, KeyInput.KEY_D, controls.moveXN.value, true);
		}
		// actions
//		InputMapperHelpers.mapKey(m, KeyInput.KEY_1, controls.action1.value);
//		InputMapperHelpers.mapKey(m, KeyInput.KEY_NUMPAD1, controls.action1.value);
//		InputMapperHelpers.mapKey(m, KeyInput.KEY_2, controls.action2.value);
//		InputMapperHelpers.mapKey(m, KeyInput.KEY_NUMPAD2, controls.action2.value);
//		InputMapperHelpers.mapKey(m, KeyInput.KEY_3, controls.action3.value);
//		InputMapperHelpers.mapKey(m, KeyInput.KEY_NUMPAD3, controls.action3.value);
//		InputMapperHelpers.mapKey(m, KeyInput.KEY_4, controls.action4.value);
//		InputMapperHelpers.mapKey(m, KeyInput.KEY_NUMPAD4, controls.action4.value);
		return m;
	}
}