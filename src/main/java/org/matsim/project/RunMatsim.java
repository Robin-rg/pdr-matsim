/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.project;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/* == DRS == */
import java.util.Set;

import org.matsim.core.config.ConfigGroup;

import at.ac.ait.matsim.drs.run.Drs;
import at.ac.ait.matsim.drs.run.DrsConfigGroup;
import at.ac.ait.matsim.drs.util.CarLinkAssigner;
import at.ac.ait.matsim.drs.util.DrsUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/* ========= */

/**
 * @author nagel
 *
 */
public class RunMatsim{

	public static void main(String[] args) {

		/* == DRS == */
		// fonctionne pas car pas dans une instance de classe RunMatsim
		Logger LOGGER = LogManager.getLogger();
		/* ========= */

		boolean scenario_drs;

		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			String config_path = "scenarios/douai/douai_config.xml";
			scenario_drs = config_path.contains("drs");
			config = ConfigUtils.loadConfig( config_path, new ConfigGroup[]{new DrsConfigGroup()} );
		} else {
			// if the config name contains "drs", set scenario_drs as true
			scenario_drs = args[0].contains("drs");
			config = ConfigUtils.loadConfig( args, new ConfigGroup[]{new DrsConfigGroup()} );
		}
		LOGGER.info("\n\nDRS activated: {} ", scenario_drs);


		config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		// possibly modify config here

		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		/* == DRS == */
		if (scenario_drs) {
			(new CarLinkAssigner(scenario.getNetwork())).run(scenario.getPopulation());
			DrsUtil.addMissingCoordsToPlanElementsFromLinks(scenario.getPopulation(), scenario.getNetwork());
			DrsUtil.addNewAllowedModeToCarLinks(scenario.getNetwork(), "drsDriver");
			int fixed = DrsUtil.addMissingDrsAffinity(scenario.getPopulation());
			if (fixed == 0) {
				LOGGER.info("All agents already had a {}, great!", "drsAffinity");
			} else {
				LOGGER.warn("For {} agents {} was missing and has been added.", fixed, "drsAffinity");
			}

			DrsUtil.addFakeGenericRouteToDrsDriverLegs(scenario.getPopulation());
			int count = DrsUtil.addDrsPlanForEligiblePlans(scenario.getPopulation(), scenario.getConfig(), "drsRider", Set.of("ride"), new String[0]);
			LOGGER.info("Added initial drs rider plan to {} agent(s)", count);
			count = DrsUtil.addDrsDriverPlans(scenario.getPopulation(), scenario.getConfig(), new String[0]);
			LOGGER.info("Added initial drs driver plan to {} agent(s)", count);
		}
		/* ========= */

		Controler controler = new Controler( scenario ) ;

		/* == DRS == */
		if (scenario_drs) {
			Drs.prepareController(controler);
		}
		/* ========= */

		controler.run();
	}
	
}
