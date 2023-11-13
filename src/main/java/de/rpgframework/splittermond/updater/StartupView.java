package de.rpgframework.splittermond.updater;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Locale;
import java.util.prefs.Preferences;

import org.update4j.Archive;
import org.update4j.Configuration;
import org.update4j.FileMetadata;
import org.update4j.UpdateContext;
import org.update4j.UpdateOptions;
import org.update4j.UpdateOptions.ArchiveUpdateOptions;
import org.update4j.UpdateResult;
import org.update4j.service.DefaultLauncher;
import org.update4j.service.DefaultUpdateHandler;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * @author prelle
 *
 */
public class StartupView extends VBox {

	private final static String PASSWORD_KEY = "patreon.password";

	private final static Logger logger = System.getLogger("mondtor.updater");

	private ChoiceBox<ReleaseType> cbType;
	private ChoiceBox<Locale> cbLang;
	private ImageView iView, logos;

	private Label lbCurrentFile;
	private ProgressBar progFiles;
	private ProgressBar progPerFile;
	private Label lbState;
	private Label lbLocalVersion, lbRemoteVersion;
	private TextField tfPassword;
	private Label hdPassword;

	private Button btnLaunch;
	private Button btnCancel;
	private Button btnUpdate;

	private Configuration config;
	private Path localConfigPath;
	private Configuration localConfig;
	private Stage primaryStage;

	private transient String currentFile;
	private byte[] configXML;

	//-------------------------------------------------------------------
	public StartupView(Stage stage) {
		this.primaryStage = stage;
		initComponents();
		initLayout();
		initInteractivity();
		cbType.setValue(ReleaseType.STABLE);
		Locale def = Locale.getDefault();
		if (def.getLanguage().equals("en")) cbLang.setValue(Locale.ENGLISH);
		if (def.getLanguage().equals("de")) cbLang.setValue(Locale.GERMAN);
		if (def.getLanguage().equals("fr")) cbLang.setValue(Locale.FRENCH);

		try {
			if (config!=null && config.requiresUpdate()) {
				System.out.println("UPDATE REQUIRED");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//-------------------------------------------------------------------
	private void initComponents() {
		iView = new ImageView(new Image(ClassLoader.getSystemResourceAsStream("figure2.png")));
		logos = new ImageView(new Image(ClassLoader.getSystemResourceAsStream("Logos.png")));

		cbType = new ChoiceBox<>();
		cbType.getItems().addAll(ReleaseType.values());
		cbType.setConverter(new StringConverter<ReleaseType>() {
			public String toString(ReleaseType val) {
				if (val==ReleaseType.STABLE) return "Stabil";
				if (val==ReleaseType.TESTING) return "Betatest";
				if (val==ReleaseType.DEVELOP) return "Entwickler";
				return (val!=null)?val.name():"?";
			}
			public ReleaseType fromString(String string) { return null;}
		});

		cbLang = new ChoiceBox<>();
		cbLang.getItems().addAll(Locale.GERMAN);
		cbLang.setConverter(new StringConverter<Locale>() {
			public String toString(Locale val) {
				if (val==Locale.ENGLISH) return "English";
				if (val==Locale.GERMAN) return "Deutsch";
				if (val==Locale.FRENCH) return "Français";
				return (val!=null)?val.getLanguage():"?";
			}
			public Locale fromString(String string) { return null;}
		});

		lbCurrentFile = new Label();
		lbLocalVersion = new Label();
		lbRemoteVersion = new Label();
		tfPassword = new PasswordField();
		tfPassword.setPromptText("Patreon Password");
		String oldPW = Preferences.userRoot().get(PASSWORD_KEY, null);
		if (oldPW!=null)
			tfPassword.setText(oldPW);

		progFiles = new ProgressBar(0.0f);
		progPerFile = new ProgressBar(0.0f);
		lbState   = new Label();
		lbState.setWrapText(true);

		btnLaunch = new Button("Anwendung starten");
		btnUpdate = new Button("Aktualisieren");
		btnCancel = new Button("Abbrechen");
		btnLaunch.setDisable(true);
	}

	//-------------------------------------------------------------------
	private void initLayout() {
		setStyle("-fx-background-color: rgba(0,0,0,0);");

		GridPane grid = new GridPane();
		grid.setVgap(10);
		grid.setHgap(10);
		Label hdType = new Label("Art");
		Label hdLang = new Label("Sprache");
		Label hdLocal  = new Label("Installiert:");
		Label hdRemote = new Label("Verfügbar:");
		hdPassword = new Label("Patreon Passwort:");
		hdType.setStyle("-fx-font-weight: bold");
		hdLang.setStyle("-fx-font-weight: bold");
		hdLocal.setStyle("-fx-font-weight: bold");
		hdRemote.setStyle("-fx-font-weight: bolder");
		hdPassword.setStyle("-fx-font-weight: bolder");

		grid.add(hdType, 0, 0);
		grid.add(cbType, 1, 0);
//		grid.add(lbLang, 0, 1);
//		grid.add(cbLang, 1, 1);
		grid.add(new HBox(5,hdLocal, lbLocalVersion), 0, 2);
		grid.add(new HBox(5,hdRemote, lbRemoteVersion), 1, 2);
		grid.add(hdPassword, 0, 3);
		grid.add(tfPassword, 1, 3);
		grid.add(progFiles    , 0, 4, 2,1);
		grid.add(progPerFile  , 0, 5, 2,1);
		grid.add(lbState, 0, 6, 2,1);

		progPerFile.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(progPerFile, true);
		progFiles.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(progFiles, true);

		GridPane.setFillHeight(hdLang, true);
		GridPane.setHalignment(btnUpdate, HPos.CENTER);
		HBox.setMargin(iView, new Insets(-250, 0, 0, 0));
		GridPane.setHalignment(hdType, HPos.RIGHT);
		GridPane.setHalignment(hdLang, HPos.RIGHT);

		grid.getColumnConstraints().add(new ColumnConstraints(180));
		grid.getColumnConstraints().add(new ColumnConstraints(180));
		//getRowConstraints().add(new RowConstraints(250));

		Region buf2 = new Region();
		buf2.setPrefHeight(50);
		HBox lowButtons = new HBox(10, btnLaunch, btnCancel);
		lowButtons.setAlignment(Pos.CENTER);

		VBox gridPlusButtons = new VBox(20,grid,buf2,btnUpdate, lowButtons);
		VBox.setVgrow(lowButtons, Priority.NEVER);
		HBox.setMargin(gridPlusButtons, new Insets(4));
		gridPlusButtons.setAlignment(Pos.TOP_CENTER);

		HBox line = new HBox(20);
		line.getChildren().addAll(gridPlusButtons, iView);
		line.setStyle("-fx-background-color: white; -fx-background-radius: 30px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 10, 0.5, 0.0, 0.0);");
		iView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 10, 0.5, 0.0, 0.0);");
		logos.setStyle("-fx-effect: dropshadow(gaussian, rgba(255, 255, 255, 0.8), 10, 0.5, 0.0, 0.0);");

		VBox.setMargin(logos, new Insets(114,0,0,20));
		getChildren().addAll(logos,line);
		HBox.setMargin(grid, new Insets(10,0,0,0));
		VBox.setMargin(line, new Insets(15));
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
		cbLang.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> Locale.setDefault(n));

		cbType.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Updated stability type to "+n);
			if (n==null) return;

			if (n==ReleaseType.DEVELOP) {
				tfPassword.setVisible(true);
				hdPassword.setVisible(true);
			} else {
				tfPassword.setVisible(false);
				hdPassword.setVisible(false);
			}
			try {
				updateLocalConfig();
				updateRemoteConfig();
				if (config==null) return;

				logger.log(Level.INFO, "Config now "+config.getResolvedProperty("project.version"));
				if (config.requiresUpdate()) {
					logger.log(Level.INFO, "Update required");
					btnUpdate.setText("Update to "+config.getResolvedProperty("project.version"));
					btnUpdate.setDisable(false);
					btnLaunch.setDisable(true);
				} else {
					logger.log(Level.INFO, "Update not required");
					if (localConfig==null) {
						logger.log(Level.INFO, "No local config "+localConfigPath);
						Files.createDirectories(localConfigPath.getParent());
						Files.write(localConfigPath, configXML);
					}
					btnUpdate.setDisable(true);
					btnLaunch.setDisable(false);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		btnUpdate.setOnAction(ev -> {
			try {
				update();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		btnLaunch.setOnAction(ev -> {
			logger.log(Level.INFO, "Launcher "+config.getLauncher());
			logger.log(Level.INFO, "Launch "+config.getResolvedProperty(DefaultLauncher.MAIN_CLASS_PROPERTY_KEY));
			System.setProperty("profile", cbType.getValue().name().toLowerCase());
			System.setProperty("eden-server", cbType.getValue().server);
			getScene().getWindow().hide();
			((Stage)getScene().getWindow()).close();
			System.setProperty("project.version", config.getResolvedProperty("project.version"));
			MondtorLauncher.primaryStage = primaryStage;
			logger.log(Level.INFO, "Call config.launch()");
			config.launch();
		});

		btnCancel.setOnAction(ev -> {
			Platform.exit();
			System.exit(0);
		});

		tfPassword.textProperty().addListener( (ov,o,n) -> {
			try {
				Preferences.userRoot().put(PASSWORD_KEY, n);
				updateRemoteConfig();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	//-------------------------------------------------------------------
	private void updateLocalConfig() throws IOException {
		localConfigPath = Paths.get(System.getProperty("user.home"), "Mondtor", "app", cbType.getValue().name().toLowerCase(), "config.xml");
		logger.log(Level.INFO, "Expect local config at {0}", localConfigPath);
		lbState.setText("Installiert unter: "+localConfigPath.getParent());
		if (!Files.exists(localConfigPath)) {
			localConfig = null;
			logger.log(Level.INFO, "No local config at {0}", localConfigPath);
			Platform.runLater( () -> lbLocalVersion.setText("None"));
			btnLaunch.setDisable(true);
			return;
		}
		logger.log(Level.INFO, "Found local config at {0}", localConfigPath);
		try {
			localConfig = Configuration.read(new FileReader(localConfigPath.toFile()));
			Platform.runLater( () -> lbLocalVersion.setText(localConfig.getResolvedProperty("project.version")));
			btnLaunch.setDisable(!Files.exists(localConfigPath));
		} catch (IOException e) {
			logger.log(Level.ERROR, "Cannot load local config: "+e.getMessage());
			btnLaunch.setDisable(true);
		}
	}

	//-------------------------------------------------------------------
	private String getBasicAuthenticationHeader() {
	    String valueToEncode = "patreon:" + tfPassword.getText();
	    return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
	}

	//-------------------------------------------------------------------
	private void updateRemoteConfig() throws IOException {
		ReleaseType n = cbType.getValue();
		//String credentials = "patreon:"+tfPassword.getText()+"@";
		String credentials ="";
		URL configUrl = new URL("http://"+credentials+n.getServer()+"/mondtor-updates/"+n.name().toLowerCase()+"/config.xml");
		logger.log(Level.DEBUG, "Search config at {0}", configUrl);
		config = null;
		try {
			HttpResponse<byte[]> response = HttpClient
					.newBuilder()
					.build()
					.send(HttpRequest.newBuilder()
							.uri(configUrl.toURI())
							.header("Authorization", getBasicAuthenticationHeader())
							.build(), HttpResponse.BodyHandlers.ofByteArray());
			switch (response.statusCode()) {
			case 200:
				configXML = response.body();
				logger.log(Level.DEBUG, "Config successfully loaded with {0} bytes",configXML.length);
				btnUpdate.setDisable(false);
				break;
			case 404:
				logger.log(Level.DEBUG, "No config for {0} exists on remote server",n);
				btnUpdate.setDisable(true);
				return;
			default:
				btnUpdate.setDisable(true);
				if (response.statusCode()==401) {
					btnLaunch.setDisable(true);
				} else {
					logger.log(Level.ERROR, "Error loading config for {0} from remote server: {1}",n, response.statusCode());
					btnLaunch.setDisable(false);
				}
				return;
			}
			Reader in = new InputStreamReader(new ByteArrayInputStream(configXML));
			config = Configuration.read(in);
			Platform.runLater( () -> lbRemoteVersion.setText(config.getResolvedProperty("project.version")));

		} catch (Exception e) {
			logger.log(Level.DEBUG, "No config for {0} found at {1}",n.name().toLowerCase(), configUrl);
			btnUpdate.setDisable(true);
			Path local = Paths.get("../config.xml").toAbsolutePath();
			System.err.println("Could not load remote config, falling back to local: "+local);
			try (Reader in = Files.newBufferedReader(local)) {
				config = Configuration.read(in);
			} catch (Exception ee) {
				logger.log(Level.DEBUG, "Fallback config not found");
				return;
			}
		}

	}

	//-------------------------------------------------------------------
	public ReadOnlyObjectProperty<ReleaseType> releaseTypeProperty() {
		return cbType.getSelectionModel().selectedItemProperty();
	}

	private void update() throws IOException {
		logger.log(Level.DEBUG,"Base = "+config.getBasePath()+" from "+config.getBaseUri());
		Path zip = Files.createTempDirectory("mondtor").resolve(cbType.getValue().name().toLowerCase()+".zip");
		logger.log(Level.DEBUG,"Archive to "+zip);
		ArchiveUpdateOptions options = UpdateOptions.archive(zip);
		options.updateHandler(new DefaultUpdateHandler() {
		    @Override
		    public void startDownloads() throws Throwable {
		    	logger.log(Level.INFO, "startDownloads");
		    	Platform.runLater( () -> btnLaunch.setDisable(true));
		    }
		    @Override
		    public void doneDownloads() throws Throwable {
		    	logger.log(Level.INFO, "doneDownloads");
		    }
		    @Override
		    public void succeeded() {
		    	logger.log(Level.INFO, "succeeded");
		    	btnLaunch.setDisable(false);
		    }

		    public void updateDownloadProgress(float frac) throws Throwable {
		        Platform.runLater( () -> progFiles.setProgress(frac));
		    }

		    @Override
		    public void updateDownloadFileProgress(FileMetadata file, float frac) throws Throwable {
		        super.updateDownloadFileProgress(file, frac);
		        currentFile = file.getPath().getFileName().toString();
		        Platform.runLater( () -> progPerFile.setProgress(frac));
		    }
		    public InputStream openDownloadStream(FileMetadata file) throws Throwable {
		    	logger.log(Level.INFO, "openDownloadStream "+file.getUri().toString());
		        currentFile = file.getPath().getFileName().toString();
		        Platform.runLater( () -> lbCurrentFile.setText(currentFile))	;

		        URLConnection connection = file.getUri().toURL().openConnection();

		        // Some downloads may fail with HTTP/403, this may solve it
		        connection.addRequestProperty("User-Agent", "Mondtor Updater");
		        // Set a connection timeout of 10 seconds
		        connection.setConnectTimeout(10 * 1000);
		        // Set a read timeout of 10 seconds
		        connection.setReadTimeout(10 * 1000);

		        return connection.getInputStream();
		    }

		});
		Thread thread = new Thread( () -> {
			SSLFix.execute();
			UpdateResult result = config.update(options);
			logger.log(Level.ERROR, "config.update result "+result);
			UpdateContext obj = result.result();
			if (result.getException()==null) {
				logger.log(Level.INFO, "All files downloaded and packaged as an archive ready to install");
				try {
					Archive.read(zip).install(true);
					Files.createDirectories(localConfigPath.getParent());
					Files.write(localConfigPath, configXML);
					updateLocalConfig();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				logger.log(Level.ERROR, "Update failed",result.getException());
				Platform.runLater( () -> lbState.setText("Update failed for "+currentFile))	;
			}
		});
		thread.start();
	}
}
