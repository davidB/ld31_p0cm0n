/// License [CC0](http://creativecommons.org/publicdomain/zero/1.0/)
package ld31;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import javax.inject.Inject;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor=@__(@Inject))
public class HudInGame {
//	@FXML
//	public Region root;
	@FXML
	public Button settings;

	@FXML
	public Button quit;

	@FXML
	public TextArea consoleLog;

	@FXML
	public Label pelletCount;

	@FXML
	public Label timeCount;
}
