/// License [CC0](http://creativecommons.org/publicdomain/zero/1.0/)
package ld31;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javax.inject.Inject;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor=@__(@Inject))
public class HudEnd {
//	@FXML
//	public Region root;
	@FXML
	public Label time;

	@FXML
	public Label timeCount;

	@FXML
	public Button retry;
}
