/// License [CC0](http://creativecommons.org/publicdomain/zero/1.0/)
package ld31;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.util.StringConverter;

import javax.inject.Inject;

import jme3_ext.AppSettingsLoader;
import jme3_ext.AudioManager;
import jme3_ext.InputMapper;
import jme3_ext.InputTextureFinder;
import lombok.RequiredArgsConstructor;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;

@RequiredArgsConstructor(onConstructor=@__(@Inject))
public class HudSettings implements Initializable {
	final AppSettingsLoader loader;
	final AudioManager audio;
	final InputMapper inputMapper;
	final Commands commands;
	final InputTextureFinder inputTextureFinders;

	private ResourceBundle resources;

	//	@FXML
	//	public Region root;
	@FXML
	public Label title;

	@FXML
	public Button back;

	@FXML
	public ChoiceBox<Integer> antialiasing;

	@FXML
	public CheckBox fullscreen;

	@FXML
	public ChoiceBox<DisplayMode> resolution;

	@FXML
	public CheckBox showFps;

	@FXML
	public CheckBox showStats;

	@FXML
	public Slider audioMasterVolume;

	@FXML
	public Slider audioMusicVolume;

	@FXML
	public Slider audioSoundVolume;

	@FXML
	public CheckBox vsync;

	@FXML
	public Button applyVideo;

	@FXML
	public Button audioSoundTest;

	@FXML
	public Button audioMusicTest;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.resources = resources;
		antialiasing.setConverter(new StringConverter<Integer>() {
			ResourceBundle resourceBundle = ResourceBundle.getBundle("com.jme3.app/SettingsDialog");

			@Override
			public String toString(Integer input) {
				return (input == 0)? resourceBundle.getString("antialias.disabled") : input +"x";
			}

			@Override
			public Integer fromString(String string) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		resolution.setConverter(new StringConverter<DisplayMode>() {
			@Override
			public String toString(DisplayMode input) {
				String dp = (input.getBitDepth() > 0) ? String.valueOf(input.getBitDepth()) : "??";
				String freq = (input.getRefreshRate() != DisplayMode.REFRESH_RATE_UNKNOWN) ? String.valueOf(input.getRefreshRate()) : "??";
				return String.format("%dx%d (%s bit) %sHz", input.getWidth(), input.getHeight(), dp, freq);
			}

			@Override
			public DisplayMode fromString(String string) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		antialiasing.valueProperty().addListener((v,o,n) -> applyVideo.setDisable(false));
		fullscreen.selectedProperty().addListener((v,o,n) -> applyVideo.setDisable(false));
		resolution.valueProperty().addListener((v,o,n) -> applyVideo.setDisable(false));
		showFps.selectedProperty().addListener((v,o,n) -> applyVideo.setDisable(false));
		showStats.selectedProperty().addListener((v,o,n) -> applyVideo.setDisable(false));
		vsync.selectedProperty().addListener((v,o,n) -> applyVideo.setDisable(false));

		audioMasterVolume.setMax(1.0);
		audioMasterVolume.setMin(0.0);
		audioMusicVolume.setMax(1.0);
		audioMusicVolume.setMin(0.0);
		audioSoundVolume.setMax(1.0);
		audioSoundVolume.setMin(0.0);
		audioSoundTest.setDisable(true);
		audioMusicTest.setDisable(true);
	}

	public void load(SimpleApplication app) {
		final AppSettings settingsInit = new AppSettings(false);
		settingsInit.copyFrom(app.getContext().getSettings());
		loadDisplayModes(settingsInit);
		fullscreen.setSelected(settingsInit.isFullscreen());
		vsync.setSelected(settingsInit.isVSync());
		showStats.setSelected(false);
		showFps.setSelected(true);
		loadAntialias(settingsInit);
		Runnable saveSettings = () -> {
			try {
				loader.save(app.getContext().getSettings());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};

		applyVideo.onActionProperty().set((v) -> {
			apply(app);
			saveSettings.run();
			applyVideo.setDisable(true);
		});
		applyVideo.setDisable(true);

		//TODO save when tab lost focus
		audioMasterVolume.valueProperty().bindBidirectional(audio.masterVolume);
		audioMusicVolume.valueProperty().bindBidirectional(audio.musicVolume);
		audioSoundVolume.valueProperty().bindBidirectional(audio.soundVolume);
		audio.loadFromAppSettings();
		ChangeListener<Boolean> saveAudio = (v,o,n) -> {
			// on lost focus
			if (!n) {
				audio.saveIntoAppSettings();
				saveSettings.run();
			}
		};
		audioMasterVolume.focusedProperty().addListener(saveAudio);
		audioMusicVolume.focusedProperty().addListener(saveAudio);
		audioSoundVolume.focusedProperty().addListener(saveAudio);
	}

	void loadDisplayModes(AppSettings settings0) {
		List<DisplayMode> r = new LinkedList<DisplayMode>();
		r.add(new DisplayMode(settings0.getWidth(), settings0.getHeight(), settings0.getDepthBits(), settings0.getFrequency()));
		//r.add(new DisplayMode(800, 600, 24, 60));
		//r.add(new DisplayMode(1024, 768, 24, 60));
		//r.add(new DisplayMode(1280, 720, 24, 60));
		//r.add(new DisplayMode(1280, 1024, 24, 60));
		//    	for(GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		for (DisplayMode mode : device.getDisplayModes()) {
			r.add(mode);
		}
		//    	}
		Collections.sort(r, new Comparator<DisplayMode>(){
			@Override
			public int compare(DisplayMode d1, DisplayMode d2) {
				int r = 0;
				if (d1.getWidth() > d2.getWidth()) {
					r = 1;
				} else if (d1.getHeight() > d2.getHeight()) {
					r = 1;
				} else if (d1.getWidth() != d2.getWidth() || d1.getHeight() != d2.getHeight()) {
					r = -1;
				}
				return r;
			}
		});
		resolution.itemsProperty().get().clear();
		resolution.itemsProperty().get().setAll(r);

		DisplayMode current = null;
		for(int i = 0; i < r.size(); i++) {
			DisplayMode mode = r.get(i);
			if (mode.getWidth() == settings0.getWidth() && mode.getHeight() == settings0.getHeight()) {
				current = mode;
			}
		}
		resolution.valueProperty().set(current);
	}

	void loadAntialias(AppSettings settings0) {
		List<Integer> r = new LinkedList<Integer>();
		r.add(0);
		r.add(2);
		r.add(4);
		r.add(6);
		r.add(8);
		r.add(16);
		antialiasing.itemsProperty().get().clear();
		antialiasing.itemsProperty().get().setAll(r);
		antialiasing.valueProperty().set(Math.max(settings0.getSamples(), 0));
	}

	void apply(SimpleApplication app) {
		try {
			app.setDisplayFps(showFps.isSelected());
			app.setDisplayStatView(showStats.isSelected());
			app.setShowSettings(true);
			AppSettings settingsEdit = new AppSettings(false);
			settingsEdit.copyFrom(app.getContext().getSettings());
			settingsEdit.setFullscreen(fullscreen.isSelected());
			settingsEdit.setVSync(vsync.isSelected());
			DisplayMode mode = resolution.getValue();
			settingsEdit.setResolution(mode.getWidth(), mode.getHeight());
			settingsEdit.setDepthBits(mode.getBitDepth());
			settingsEdit.setFrequency(!vsync.isSelected() ? 0 : (mode.getRefreshRate() != DisplayMode.REFRESH_RATE_UNKNOWN)? mode.getRefreshRate() : 60);
			settingsEdit.setSamples(antialiasing.getValue());
			app.setSettings(settingsEdit);
			settingsEdit.save(settingsEdit.getTitle());
			app.restart();
//	    	((Main)app).onNextReshape = new Function<Main,Boolean>(){
//				@Override
//				public Boolean apply(Main input) {
//		    		Widgets.fullCamera(hudPanel, input.getCamera());
//		    		return true;
//				}
//
//	    	};
		} catch(Exception exc) {
			throw new RuntimeException(exc);
		}
	}
}
