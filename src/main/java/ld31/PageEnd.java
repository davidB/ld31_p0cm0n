/// License [CC0](http://creativecommons.org/publicdomain/zero/1.0/)
package ld31;

import javax.inject.Inject;

import jme3_ext.AppState0;
import jme3_ext.Hud;
import jme3_ext.HudTools;
import jme3_ext.InputMapper;
import lombok.RequiredArgsConstructor;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import com.jme3x.jfx.FxPlatformExecutor;

/**
 *
 * @author David Bernard
 */
@RequiredArgsConstructor(onConstructor=@__(@Inject))
class PageEnd extends AppState0 {
	private final HudTools hudTools;
	private final InputMapper inputMapper;
	private final Commands controls;

	private boolean prevCursorVisible;
	private Hud<HudEnd> hud;
	private Subscription inputSub;

	@Override
	public void doInitialize() {
		hud = hudTools.newHud("Interface/HudEnd.fxml", new HudEnd());
	}
	@Override
	protected void doEnable() {
		prevCursorVisible = app.getInputManager().isCursorVisible();
		app.getInputManager().setCursorVisible(true);
		app.getInputManager().addRawInputListener(inputMapper.rawInputListener);
		hudTools.show(hud);

		FxPlatformExecutor.runOnFxApplication(() -> {
			HudEnd p = hud.controller;
			p.timeCount.setText(String.format("%d",app.getStateManager().getState(PageInGame.class).score()));
			p.retry.onActionProperty().set((v) -> {
				app.enqueue(()-> {
					app.getStateManager().getState(PageInGame.class).reset();
					app.getStateManager().detach(this);
					return true;
				});
			});
		});

		inputSub = Subscriptions.from(
			controls.exit.value.subscribe((v) -> {
				if (!v) hud.controller.retry.fire();
			})
			,controls.def.value.subscribe((v) -> {
				if (!v) hud.controller.retry.fire();
			})
		);
	}

	@Override
	protected void doDisable() {
		hudTools.hide(hud);
		app.getInputManager().setCursorVisible(prevCursorVisible);
		app.getInputManager().removeRawInputListener(inputMapper.rawInputListener);
		if (inputSub != null){
			inputSub.unsubscribe();
			inputSub = null;
		}
	}
}
