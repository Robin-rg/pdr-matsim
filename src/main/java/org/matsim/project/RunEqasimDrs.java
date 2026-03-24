package org.matsim.project;

import org.eqasim.core.scenario.validation.VehiclesValidator;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/* == DRS == */
import java.util.Set;

//import org.matsim.core.config.ConfigGroup; // déjà importé

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import at.ac.ait.matsim.drs.util.CarLinkAssigner;
import at.ac.ait.matsim.drs.util.DrsUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/* ========= */

public class RunEqasimDrs {
    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = (new CommandLine.Builder(args))
            .requireOptions(new String[]{"config-path"})
            .allowPrefixes(new String[]{"mode-choice-parameter", "cost-parameter"})
            .build();

        IDFConfigurator configurator = new IDFConfigurator(cmd);
        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), new ConfigGroup[]{new DrsConfigGroup()} ); // new ConfigGroup[0]
        configurator.updateConfig(config);

        cmd.applyConfiguration(config);
        VehiclesValidator.validate(config);

        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);
        configurator.adjustScenario(scenario);

        /* == DRS == */
        (new CarLinkAssigner(scenario.getNetwork())).run(scenario.getPopulation());
        DrsUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());
        DrsUtil.addNewAllowedModeToCarLinks(scenario.getNetwork(), "drsDriver");
        int fixed = DrsUtil.addMissingDrsAffinity(scenario.getPopulation());
        if (fixed == 0) {
            //LOGGER.info("All agents already had a {}, great!", "drsAffinity");
        } else {
            //LOGGER.warn("For {} agents {} was missing and has been added.", fixed, "drsAffinity");
        }

        DrsUtil.addFakeGenericRouteToDrsDriverLegs(scenario.getPopulation());
        int count = DrsUtil.addDrsPlanForEligiblePlans(scenario.getPopulation(), scenario.getConfig(), "drsRider", Set.of("ride"), new String[0]);
        //LOGGER.info("Added initial drs rider plan to {} agent(s)", count);
        count = DrsUtil.addDrsDriverPlans(scenario.getPopulation(), scenario.getConfig(), new String[0]);
        //LOGGER.info("Added initial drs driver plan to {} agent(s)", count);
        /* ========= */

        Controler controller = new Controler(scenario);
        configurator.configureController(controller);

        /* == DRS == */
        Drs.prepareController(controller);
        /* ========= */

        controller.run();
    }
}
