/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* This file is part of CCT. For details, see https://github.com/LLNL/coda-calibration-tool. 
* 
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.calibration.gui.controllers;

import java.io.IOException;

import gov.llnl.gnem.apps.coda.calibration.gui.util.ProgressMonitor;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

/**
 * @throws IllegalStateException
 *             if the FXML sub-system is unable to initialize the display
 */
public class ProgressGui {

    @FXML
    private TableView<ProgressMonitor> progressTable;

    @FXML
    private TableColumn<ProgressMonitor, Node> progressColumn;

    @FXML
    private TableColumn<ProgressMonitor, String> taskColumn;

    private Stage stage;
    private ObservableList<ProgressMonitor> monitors = FXCollections.observableArrayList();

    public ProgressGui() {
        Platform.runLater(() -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ProgressDisplay.fxml"));
            fxmlLoader.setController(this);
            stage = new Stage(StageStyle.UTILITY);
            Font.loadFont(getClass().getResource("/fxml/MaterialIcons-Regular.ttf").toExternalForm(), 18);
            Parent root;
            try {
                root = fxmlLoader.load();
                Scene scene = new Scene(root);
                stage.setScene(scene);

                taskColumn.setCellValueFactory(value -> Bindings.createStringBinding(() -> value.getValue().getDisplayableName()));

                progressColumn.setCellValueFactory(value -> Bindings.createObjectBinding(() -> value.getValue()));
                progressColumn.setCellFactory(new Callback<TableColumn<ProgressMonitor, Node>, TableCell<ProgressMonitor, Node>>() {

                    @Override
                    public TableCell<ProgressMonitor, Node> call(TableColumn<ProgressMonitor, Node> param) {
                        return new TableCell<ProgressMonitor, Node>() {
                            @Override
                            protected void updateItem(Node item, boolean empty) {
                                if (item == getItem())
                                    return;
                                super.updateItem(item, empty);

                                if (item == null) {
                                    super.setText(null);
                                    super.setGraphic(null);
                                } else if (item instanceof Node) {
                                    super.setText(null);
                                    super.setGraphic((Node) item);
                                }
                            }
                        };
                    }
                });
                progressTable.setItems(monitors);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public void addProgressMonitor(ProgressMonitor monitor) {
        monitors.add(monitor);
        Platform.runLater(() -> {
            stage.show();
        });
    }

    public void removeProgressMonitor(ProgressMonitor monitor) {
        monitors.remove(monitor);
    }

    public void hide() {
        Platform.runLater(() -> {
            stage.hide();
        });
    }

    public void show() {
        Platform.runLater(() -> {
            stage.show();
        });
    }
}
