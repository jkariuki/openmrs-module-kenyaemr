/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.kenyaemr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.calculation.CalculationManager;
import org.openmrs.module.kenyaemr.form.FormManager;
import org.openmrs.module.kenyaemr.identifier.IdentifierManager;
import org.openmrs.module.kenyaemr.lab.LabManager;
import org.openmrs.module.kenyaemr.regimen.RegimenManager;
import org.openmrs.module.kenyaemr.reporting.ReportManager;
import org.openmrs.module.kenyaemr.util.BuildProperties;
import org.openmrs.ui.framework.UiContextRefreshedCallback;
import org.openmrs.ui.framework.fragment.FragmentFactory;
import org.openmrs.ui.framework.page.PageFactory;
import org.openmrs.ui.framework.resource.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kenya EMR master content manager, used as a registered singleton
 */
@Component
public class KenyaEmr implements UiContextRefreshedCallback {

	protected static final Log log = LogFactory.getLog(KenyaEmrActivator.class);

	@Autowired
	private MetadataManager metadataManager;

	@Autowired
	private IdentifierManager identifierManager;

	@Autowired
	private RegimenManager regimenManager;

	@Autowired
	private FormManager formManager;

	@Autowired
	private CalculationManager calculationManager;

	@Autowired
	private ReportManager reportManager;

	@Autowired
	private LabManager labManager;

	/**
	 * Gets the module version
	 * @return the version
	 */
	public String getModuleVersion() {
		return ModuleFactory.getModuleById(KenyaEmrConstants.MODULE_ID).getVersion();
	}

	/**
	 * Gets the module build properties
	 * @return the build properties map or null if not available
	 */
	public BuildProperties getModuleBuildProperties() {
		return getSingletonComponent(BuildProperties.class);
	}

	/**
	 * Gets the metadata manager
	 * @return the metadata manager
	 */
	public MetadataManager getMetadataManager() {
		return metadataManager;
	}

	/**
	 * Gets the identifier manager
	 * @return the identifier manager
	 */
	public IdentifierManager getIdentifierManager() {
		return identifierManager;
	}

	/**
	 * Gets the regimen manager
	 * @return the regimen manager
	 */
	public RegimenManager getRegimenManager() {
		return regimenManager;
	}

	/**
	 * Gets the form manager
	 * @return the form manager
	 */
	public FormManager getFormManager() {
		return formManager;
	}

	/**
	 * Gets the calculation manager
	 * @return the calculation manager
	 */
	public CalculationManager getCalculationManager() {
		return calculationManager;
	}

	/**
	 * Gets the report manager
	 * @return the report manager
	 */
	public ReportManager getReportManager() {
		return reportManager;
	}

	/**
	 * Gets the lab manager
	 * @return the lab manager
	 */
	public LabManager getLabManager() {
		return labManager;
	}

	/**
	 * Gets the EMR service
	 * @return the service
	 */
	public KenyaEmrService getEmrServicee() {
		return Context.getService(KenyaEmrService.class);
	}

	/**
	 * Utility method to get the singleton instance from the application context in situations where you
	 * can't use @Autowired or @SpringBean. Use as a last resort.
	 * @return the singleton instance
	 */
	public static KenyaEmr getInstance() {
		return getSingletonComponent(KenyaEmr.class);
	}

	/**
	 * Handles a context refresh
	 */
	public void afterContextRefreshed(PageFactory pageFactory, FragmentFactory fragmentFactory, ResourceFactory resourceFactory) {
		try {
			calculationManager.refresh();
			formManager.refresh();
			reportManager.refresh();
		}
		catch (Exception ex) {
			log.error("Error during Kenya EMR context refresh", ex);
		}
	}

	/**
	 * Fetches a singleton component from the application context
	 * @param clazz the class of the component
	 * @param <T> the class of the component
	 * @return the singleton instance
	 * @throws RuntimeException if no such instance exists or more than one instance exists
	 */
	protected static <T> T getSingletonComponent(Class<T> clazz) {
		List<T> all = Context.getRegisteredComponents(clazz);
		if (all.size() == 0) {
			throw new RuntimeException("No such object in the application context");
		}
		// Because of TRUNK-3889, singleton beans get instantiated twice
		//else if (all.size() > 1) {
		//	throw new RuntimeException("Object is not a singleton in the application context");
		//}
		else {
			return all.get(0);
		}
	}
}