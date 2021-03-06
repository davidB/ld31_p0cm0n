/// License [CC0](http://creativecommons.org/publicdomain/zero/1.0/)
package ld31;

import javafx.scene.Scene;
import javafx.scene.text.Font;

import javax.inject.Inject;

import jme3_ext.PageManager;
import jme3_ext.SetupHelpers;

import com.jme3.app.SimpleApplication;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3x.jfx.FxPlatformExecutor;
import com.jme3x.jfx.GuiManager;

import dagger.ObjectGraph;

public class Main {

	private static boolean assertionsEnabled;
	private static boolean enabled() {
		Main.assertionsEnabled = true;
		return true;
	}

	public static void main(final String[] args) {
		//-Djava.util.logging.config.file=logging.properties
		SetupHelpers.installSLF4JBridge();

		assert Main.enabled();
		if (!Main.assertionsEnabled) {
			throw new RuntimeException("Assertions must be enabled (vm args -ea");
		}
		ObjectGraph injector = ObjectGraph.create(new MainModule());
		injector.get(Main.class); // Main constructor used to initialize service
	}

	//HACK to receive service without need to explicitly list them and to initialize them
	@Inject
	Main(SimpleApplication app, GuiManager guiManager, PageManager pageManager) {
		//		setAspectRatio(app, 16, 9);
		SetupHelpers.disableDefaults(app);
		SetupHelpers.setDebug(app, false);
		//SetupHelpers.logJoystickInfo(app.getInputManager());
		initGui(guiManager);
		initPages(pageManager, app, false);
	}



	static void initPages(PageManager pageManager, SimpleApplication app, boolean debug) {
		app.enqueue(() -> {
			InputManager inputManager = app.getInputManager();
			if (debug) {
				final String prefixGoto = "GOTOPAGE_";
				ActionListener a = new ActionListener() {
					public void onAction(String name, boolean isPressed, float tpf) {
						if (isPressed && name.startsWith(prefixGoto)) {
							int page = Integer.parseInt(name.substring(prefixGoto.length()));
							pageManager.goTo(page);
						};
					}
				};
				for (int i = 0; i < Pages.values().length; i++) {
					inputManager.addListener(a, prefixGoto + i);
				}
			}
			//pageManager.goTo(Pages.Welcome.ordinal());
			pageManager.goTo(Pages.InGame.ordinal());
			return true;
		});
	}

	static void initGui(GuiManager guiManager) {
		//see http://blog.idrsolutions.com/2014/04/use-external-css-files-javafx/
		Scene scene = guiManager.getjmeFXContainer().getScene();
		FxPlatformExecutor.runOnFxApplication(() -> {
			Font.loadFont(Main.class.getResource("/Fonts/Eduardo-Barrasa.ttf").toExternalForm(), 10);
			Font.loadFont(Main.class.getResource("/Fonts/bobotoh.ttf").toExternalForm(), 10);
			String css = Main.class.getResource("/Interface/main.css").toExternalForm();
			scene.getStylesheets().clear();
			scene.getStylesheets().add(css);
		});
	}


}