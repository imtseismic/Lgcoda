/*
* Copyright (c) 2018, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.envelope.standalone;

import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import gov.llnl.gnem.apps.coda.common.gui.SimpleGuiPreloader;
import gov.llnl.gnem.apps.coda.common.gui.util.CommonGuiUtils;
import gov.llnl.gnem.apps.coda.envelope.EnvelopeApplication;
import gov.llnl.gnem.apps.coda.envelope.gui.GuiApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;

@SpringBootApplication
@ComponentScan("gov.llnl.gnem.apps.coda")
public class EnvelopeStandalone extends Application {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeStandalone.class);
    private static volatile ConfigurableApplicationContext springContext;
    private static String[] initialArgs;

    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static synchronized void main(String[] args) {
        try {
            initialArgs = args;
            String preloaderName = System.getProperty("javafx.preloader");
            if (preloaderName == null) {
                System.setProperty("javafx.preloader", SimpleGuiPreloader.class.getName());
            }
            final CountDownLatch latch = new CountDownLatch(1);
            SwingUtilities.invokeLater(() -> {
                new JFXPanel(); // initializes JavaFX environment
                latch.countDown();
            });

            if (!latch.await(5L, TimeUnit.SECONDS)) {
                throw new ExceptionInInitializerError();
            }
            launch(EnvelopeStandalone.class, args);
        } catch (Exception e) {
            log.error("Exception at CodaCalibrationStandalone::main", e);
            Platform.exit();
            System.exit(1);
        }
    }

    @Override
    public void init() throws Exception {
        CommonGuiUtils.setIcon(this.getClass(), "/coda_256x256.png");
        setContext(new SpringApplicationBuilder(EnvelopeApplication.class).headless(false).run(initialArgs));
    }

    private ConfigurableApplicationContext setContext(ConfigurableApplicationContext context) {
        if (springContext != null) {
            return springContext;
        }

        springContext = context;
        return springContext;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        CommonGuiUtils.setIcon(this.getClass(), "/coda_256x256.png");
        new GuiApplication(springContext).start(primaryStage);
    }

    @Override
    public void stop() throws Exception {
        CompletableFuture.runAsync(() -> {
            springContext.stop();
            springContext.close();
        }).get(1, TimeUnit.SECONDS);
        Platform.exit();
        System.exit(0);
    }
}