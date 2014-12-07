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

import com.jme3.audio.AudioNode;
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
	private AudioNode audioGameOver;
	private AudioNode audioTryAgain;
	public boolean success = true;


	@Override
	public void doInitialize() {
		hud = hudTools.newHud("Interface/HudEnd.fxml", new HudEnd());
		audioGameOver = new AudioNode(app.getAssetManager(), "Sounds/game_over.ogg", false); // buffered
		audioGameOver.setLooping(false);
		audioGameOver.setPositional(false);
		audioTryAgain = new AudioNode(app.getAssetManager(), "Sounds/try_again.ogg", false); // buffered
		audioTryAgain.setLooping(false);
		audioTryAgain.setPositional(false);
	}

	@Override
	protected void doEnable() {
		prevCursorVisible = app.getInputManager().isCursorVisible();
		app.getInputManager().setCursorVisible(true);
		app.getInputManager().addRawInputListener(inputMapper.rawInputListener);
		hudTools.show(hud);

		FxPlatformExecutor.runOnFxApplication(() -> {
			HudEnd p = hud.controller;
			if (success) {
				p.time.setVisible(true);
				p.timeCount.setText(String.format("%d",app.getStateManager().getState(PageInGame.class).score()));
				app.enqueue(()->{
					audioTryAgain.play();
					return true;
				});
			} else {
				p.time.setVisible(false);
				p.timeCount.setText("Game Over !!");
				app.enqueue(()->{
					audioGameOver.play();
					return true;
				});
			}
			p.retry.onActionProperty().set((v) -> {
				app.enqueue(()-> {
					app.getStateManager().getState(PageInGame.class).reset();
					app.getStateManager().detach(this);
					app.getStateManager().getState(PageInGame.class).start();
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
