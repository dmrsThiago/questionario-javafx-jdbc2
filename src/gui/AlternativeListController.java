package gui;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import application.Main;
import db.DbException;
import gui.listeners.DataChangeListener;
import gui.util.Alerts;
import gui.util.Utils;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.entities.Alternative;
import model.servicies.AlternativeService;
import model.servicies.QuestionService;

public class AlternativeListController implements Initializable, DataChangeListener {
	private AlternativeService service;

	@FXML
	private TableView<Alternative> tableViewAlternative;

	@FXML
	private TableColumn<Alternative, Integer> tableColumnId;

	@FXML
	private TableColumn<Alternative, String> tableColumnDescription;
	
	@FXML
	private TableColumn<Alternative, String> tableColumnIsCorrect;

	@FXML
	private TableColumn<Alternative, Alternative> tableColumnEDIT;

	@FXML
	private TableColumn<Alternative, Alternative> tableColumnREMOVE;

	@FXML
	private Button btNew;

	private ObservableList<Alternative> obsList;
	

	@FXML
	public void onBtNewAction(ActionEvent event) {
		Stage parentStage = Utils.currentStage(event);
		Alternative obj = new Alternative();
		createDialogForm(obj, "/gui/AlternativeForm.fxml", parentStage);
	}

	public void setAlternativeService(AlternativeService service) {
		this.service = service;
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		initializeNodes();
	}

	private void initializeNodes() {
		tableColumnId.setCellValueFactory((new PropertyValueFactory<>("id")));
		tableColumnDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
		tableColumnIsCorrect.setCellValueFactory(new PropertyValueFactory<>("isCorrect"));

		Stage stage = (Stage) Main.getMainScene().getWindow();
		tableViewAlternative.prefHeightProperty().bind(stage.heightProperty());
	}

	public void updateTableView() {
		if (service == null) {
			throw new IllegalStateException("Serviço nulo");
		}

		List<Alternative> list = service.findAll();
		obsList = FXCollections.observableArrayList(list);
		tableViewAlternative.setItems(obsList);
		initEditButtons();
		initRemoveButtons();

	}

	private void createDialogForm(Alternative obj, String absoluteName, Stage parentStage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource(absoluteName));
			Pane pane = loader.load();

			AlternativeFormController controller = loader.getController();
			controller.setAlternative(obj);
			controller.setServices(new AlternativeService(), new QuestionService());
			controller.loadAssociatedObjects();
			controller.subscribeDataChangeListener(this);
			controller.updateFormData();

			Stage dialogStage = new Stage();
			dialogStage.setTitle("Entre com o conteúdo da alternativa");
			dialogStage.setScene(new Scene(pane));
			dialogStage.setResizable(false);
			dialogStage.initOwner(parentStage);
			dialogStage.initModality(Modality.WINDOW_MODAL);
			dialogStage.showAndWait();

		} catch (IOException e) {
			e.printStackTrace();
			Alerts.showAlert("IO Exception", "Error loading view", e.getMessage(), AlertType.ERROR);
		}
	}

	@Override
	public void onDataChanged() {
		updateTableView();
	}

	private void initEditButtons() {
		tableColumnEDIT.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
		tableColumnEDIT.setCellFactory(param -> new TableCell<Alternative, Alternative>() {
			private final Button button = new Button("edit");

			@Override
			protected void updateItem(Alternative obj, boolean empty) {
				super.updateItem(obj, empty);
				if (obj == null) {
					setGraphic(null);
					return;
				}
				setGraphic(button);
				button.setOnAction(event -> createDialogForm(obj, "/gui/AlternativeForm.fxml", Utils.currentStage(event)));
			}
		});
	}

	private void initRemoveButtons() {
		tableColumnREMOVE.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
		tableColumnREMOVE.setCellFactory(param -> new TableCell<Alternative, Alternative>() {
			private final Button button = new Button("remove");

			@Override
			protected void updateItem(Alternative obj, boolean empty) {
				super.updateItem(obj, empty);
				if (obj == null) {
					setGraphic(null);
					return;
				}
				setGraphic(button);
				button.setOnAction(event -> removeEntity(obj));
			}
		});
	}

	private void removeEntity(Alternative obj) {
		Optional<ButtonType> result = Alerts.showConfirmation("Confirmation", "Tem certeza que deseja deletar?");
		
		if(result.get() == ButtonType.OK) {
			if(service == null) {
				throw new IllegalStateException("Service was null");
			}
			try {
				service.remove(obj);
				updateTableView();
			}catch(DbException e) {
				Alerts.showAlert("Error removing object", null, e.getMessage(), AlertType.ERROR);
			}
		}
	}
}
