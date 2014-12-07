package ld31;

import javax.inject.Inject;

import jme3_ext.AppState0;
import lombok.RequiredArgsConstructor;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.AppStateManager;

@RequiredArgsConstructor(onConstructor=@__(@Inject))
public class AppStateDebug extends AppState0 {

	@Override
	protected void doEnable() {
		System.out.println("DEBUG ENABLE");
		AppStateManager stateManager = app.getStateManager();
		stateManager.detach(stateManager.getState(FlyCamAppState.class));
		//stateManager.attach(new StatsAppState());
		stateManager.getState(StatsAppState.class).setDisplayStatView(false);
		stateManager.getState(StatsAppState.class).setDisplayFps(true);
		stateManager.attach(new DebugKeysAppState());

		app.getInputManager().setCursorVisible(true);
		//app.getViewPort().setBackgroundColor(v? ColorRGBA.Pink : ColorRGBA.White);
//		AppStateDeferredRendering r = app.getStateManager().getState(AppStateDeferredRendering.class);
//		if (r != null) {
//			app.getStateManager().attach(new AppState4ViewDeferredTexture(r.processor, AppState4ViewDeferredTexture.ViewKey.values()));
//		}
		//Display.setResizable(v);
	}

	protected void doDispose() {
		AppStateManager stateManager = app.getStateManager();
		stateManager.detach(stateManager.getState(StatsAppState.class));
		stateManager.detach(stateManager.getState(DebugKeysAppState.class));

		System.out.println("DEBUG DISABLE");
	}
}
