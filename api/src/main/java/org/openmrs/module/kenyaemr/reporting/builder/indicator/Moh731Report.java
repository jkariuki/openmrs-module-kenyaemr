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

package org.openmrs.module.kenyaemr.reporting.builder.indicator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.kenyaemr.reporting.indicator.HivCareVisitsIndicator;
import org.openmrs.module.kenyaemr.reporting.dataset.definition.MergingDataSetDefinition;
import org.openmrs.module.kenyaemr.reporting.library.cohort.ArtCohortLibrary;
import org.openmrs.module.kenyaemr.reporting.library.cohort.CommonCohortLibrary;
import org.openmrs.module.kenyaemr.reporting.library.cohort.PwpCohortLibrary;
import org.openmrs.module.kenyaemr.reporting.library.cohort.TbCohortLibrary;
import org.openmrs.module.kenyaemr.reporting.library.dimension.CommonDimensionLibrary;
import org.openmrs.module.kenyaemr.reporting.library.indicator.ArtIndicatorLibrary;
import org.openmrs.module.kenyaemr.reporting.library.indicator.CommonIndicatorLibrary;
import org.openmrs.module.reporting.cohort.definition.*;
import org.openmrs.module.reporting.dataset.definition.CohortIndicatorDataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.SimpleIndicatorDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.indicator.CohortIndicator;
import org.openmrs.module.reporting.indicator.Indicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.openmrs.module.kenyaemr.reporting.EmrReportingUtils.map;

/**
 * MOH 731 report
 */
@Component
public class Moh731Report extends BaseIndicatorReportBuilder {

	protected static final Log log = LogFactory.getLog(Moh731Report.class);

	@Autowired
	private CommonCohortLibrary commonCohorts;

	@Autowired
	private CommonDimensionLibrary commonDimensions;

	@Autowired
	private CommonIndicatorLibrary commonIndicators;

	@Autowired
	private ArtCohortLibrary artCohorts;

	@Autowired
	private ArtIndicatorLibrary artIndicators;

	@Autowired
	private TbCohortLibrary tbCohorts;

	@Autowired
	private PwpCohortLibrary pwpCohorts;

	/**
	 * Report specific cohorts and indicators
	 */
	private Map<String, CohortDefinition> cohortDefinitions;
	private Map<String, CohortIndicator> cohortIndicators;
	private Map<String, Indicator> nonCohortIndicators;

	/**
	 * @see org.openmrs.module.kenyaemr.reporting.builder.ReportBuilder#getTags()
	 */
	@Override
	public String[] getTags() {
		return new String[] { "moh", "hiv" };
	}

	/**
	 * @see BaseIndicatorReportBuilder#getName()
	 */
	@Override
	public String getName() {
		return "MOH 731";
	}

	/**
	 * @see org.openmrs.module.kenyaemr.reporting.builder.ReportBuilder#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Comprehensive HIV/AIDS Facility Reporting Form - NASCOP";
	}

	/**
	 * @see BaseIndicatorReportBuilder#getExcelTemplateResourcePath()
	 */
	@Override
	public String getExcelTemplateResourcePath() {
		return "report_templates/Moh731Report.xls";
	}

	/**
	 * @see BaseIndicatorReportBuilder#buildDataSets()
	 */
	@Override
	public List<DataSetDefinition> buildDataSets() {
		log.debug("Setting up cohort definitions");

		setupCohortDefinitions();

		log.debug("Setting up cohort indicators");

		setupCohortIndicators();

		log.debug("Setting up non-cohort indicators");

		setupNonCohortIndicators();

		log.debug("Setting up report definition");

		return Arrays.asList(createDataSet());
	}

	private void setupCohortDefinitions() {
		cohortDefinitions = new HashMap<String, CohortDefinition>();
		{ // Started ART and is TB patient
			CompositionCohortDefinition cd = new CompositionCohortDefinition();
			cd.addParameter(new Parameter("fromDate", "From Date", Date.class));
			cd.addParameter(new Parameter("toDate", "To Date", Date.class));

			cd.addSearch("startedArt", map(artCohorts.startedArt(), "onOrAfter=${startDate},onOrBefore=${endDate}"));
			cd.addSearch("enrolledInTb", map(tbCohorts.enrolled(), "enrolledOnOrAfter=${fromDate},enrolledOnOrBefore=${toDate}"));
			cd.setCompositionString("startedArt AND enrolledInTb");
			cohortDefinitions.put("startedArtAndIsTbPatient", cd);
		}
		{ // Revisits on ART
			CompositionCohortDefinition cd = new CompositionCohortDefinition();
			cd.addParameter(new Parameter("fromDate", "From Date", Date.class));
			cd.addParameter(new Parameter("toDate", "To Date", Date.class));
			cd.addSearch("startedBefore", map(artCohorts.startedArt(), "onOrBefore=${startDate-1d}"));
			cd.addSearch("recentEncounter", map(commonCohorts.hasEncounter(), "onOrAfter=${endDate-90d},onOrBefore=${endDate}"));
			cd.setCompositionString("recentEncounter AND startedBefore");
			cohortDefinitions.put("revisitsArt", cd);
		}
		{ // Currently on ART.. we could calculate this several ways...
			CompositionCohortDefinition cd = new CompositionCohortDefinition();
			cd.addParameter(new Parameter("fromDate", "From Date", Date.class));
			cd.addParameter(new Parameter("toDate", "To Date", Date.class));
			cd.addSearch("startedArt", map(artCohorts.startedArt(), "onOrAfter=${fromDate},onOrBefore=${toDate}"));
			cd.addSearch("revisitsArt", map(cohortDefinitions.get("revisitsArt"), "fromDate=${fromDate},toDate=${toDate}"));
			cd.setCompositionString("startedArt OR revisitsArt");
			cohortDefinitions.put("currentlyOnArt", cd);
		}
		{ // Taking original 1st line ART at 12 months
			CompositionCohortDefinition cd = new CompositionCohortDefinition();
			cd.addParameter(new Parameter("fromDate", "From Date", Date.class));
			cd.addParameter(new Parameter("toDate", "To Date", Date.class));
			cd.addSearch("art12MonthNetCohort", map(artCohorts.netCohort12Months(), "onDate=${toDate}"));
			cd.addSearch("currentlyOnOriginalFirstLine", map(artCohorts.onOriginalFirstLine(), "onDate=${toDate}"));
			cd.setCompositionString("art12MonthNetCohort AND currentlyOnOriginalFirstLine");
			cohortDefinitions.put("onOriginalFirstLineAt12Months", cd);
		}
		{ // Taking alternate 1st line ART at 12 months
			CompositionCohortDefinition cd = new CompositionCohortDefinition();
			cd.addParameter(new Parameter("fromDate", "From Date", Date.class));
			cd.addParameter(new Parameter("toDate", "To Date", Date.class));
			cd.addSearch("art12MonthNetCohort", map(artCohorts.netCohort12Months(), "onDate=${toDate}"));
			cd.addSearch("currentlyOnAlternateFirstLine", map(artCohorts.onAlternateFirstLine(), "onDate=${toDate}"));
			cd.setCompositionString("art12MonthNetCohort AND currentlyOnAlternateFirstLine");
			cohortDefinitions.put("onAlternateFirstLineAt12Months", cd);
		}
		{ // Taking 2nd line ART at 12 months
			CompositionCohortDefinition cd = new CompositionCohortDefinition();
			cd.addParameter(new Parameter("fromDate", "From Date", Date.class));
			cd.addParameter(new Parameter("toDate", "To Date", Date.class));
			cd.addSearch("art12MonthNetCohort", map(artCohorts.netCohort12Months(), "onDate=${toDate}"));
			cd.addSearch("currentlyOnSecondLine", map(artCohorts.onSecondLine(), "onDate=${toDate}"));
			cd.setCompositionString("art12MonthNetCohort AND currentlyOnSecondLine");
			cohortDefinitions.put("onSecondLineAt12Months", cd);
		}
		{ // Taking any ART at 12 months
			CompositionCohortDefinition cd = new CompositionCohortDefinition();
			cd.addParameter(new Parameter("fromDate", "From Date", Date.class));
			cd.addParameter(new Parameter("toDate", "To Date", Date.class));
			cd.addSearch("art12MonthNetCohort", map(artCohorts.netCohort12Months(), "onDate=${toDate}"));
			cd.addSearch("currentlyOnArt", map(artCohorts.onArt(), "onDate=${toDate}"));
			cd.setCompositionString("art12MonthNetCohort AND currentlyOnArt");
			cohortDefinitions.put("onTherapyAt12Months", cd);
		}
	}

	private void setupCohortIndicators() {
		cohortIndicators = new HashMap<String, CohortIndicator>();
		{
			CohortIndicator ind = createCohortIndicator("currentlyInCare", "Currently in care (includes transfers)");
			ind.setCohortDefinition(map(commonCohorts.hasEncounter(), "onOrAfter=${endDate-90d},onOrBefore=${endDate}"));
		}
		{
			CohortIndicator ind = createCohortIndicator("startingArtTbPatient", "Starting ART (TB Patient)");
			ind.setCohortDefinition(map(cohortDefinitions.get("startedArtAndIsTbPatient"), "fromDate=${startDate},toDate=${endDate}"));
		}
		{
			CohortIndicator ind = createCohortIndicator("revisitsArt", "Revisits ART");
			ind.setCohortDefinition(map(cohortDefinitions.get("revisitsArt"), "fromDate=${startDate},toDate=${endDate}"));
		}
		{
			CohortIndicator ind = createCohortIndicator("currentlyOnArt", "Currently on ART");
			ind.setCohortDefinition(map(cohortDefinitions.get("currentlyOnArt"), "fromDate=${startDate},toDate=${endDate}"));
		}
		{
			CohortIndicator ind = createCohortIndicator("cumulativeOnArt", "Cumulative ever on ART");
			ind.setCohortDefinition(map(artCohorts.startedArt(), "onOrBefore=${endDate}"));
		}
		{
			CohortIndicator ind = createCohortIndicator("art12MonthNetCohort", "ART 12 Month Net Cohort");
			ind.setCohortDefinition(map(artCohorts.netCohort12Months(), "onDate=${endDate}"));
		}
		{
			CohortIndicator ind = createCohortIndicator("onOriginalFirstLineAt12Months", "On original 1st line at 12 months");
			ind.setCohortDefinition(map(cohortDefinitions.get("onOriginalFirstLineAt12Months"), "fromDate=${startDate},toDate=${endDate}"));
		}
		{
			CohortIndicator ind = createCohortIndicator("onAlternateFirstLineAt12Months", "On alternate 1st line at 12 months");
			ind.setCohortDefinition(map(cohortDefinitions.get("onAlternateFirstLineAt12Months"), "fromDate=${startDate},toDate=${endDate}"));
		}
		{
			CohortIndicator ind = createCohortIndicator("onSecondLineAt12Months", "On 2nd line at 12 months");
			ind.setCohortDefinition(map(cohortDefinitions.get("onSecondLineAt12Months"), "fromDate=${startDate},toDate=${endDate}"));
		}
		{
			CohortIndicator ind = createCohortIndicator("onTherapyAt12Months", "On therapy at 12 months");
			ind.setCohortDefinition(map(cohortDefinitions.get("onTherapyAt12Months"), "fromDate=${startDate},toDate=${endDate}"));
		}
		{
			CohortIndicator ind = createCohortIndicator("screenedForTb", "Screened for TB");
			ind.setCohortDefinition(map(tbCohorts.screenedForTb(), "onOrAfter=${startDate},onOrBefore=${endDate}"));
		}
		{
			CohortIndicator ind = createCohortIndicator("modernContraceptivesProvided", "Modern contraceptives provided");
			ind.setCohortDefinition(map(pwpCohorts.modernContraceptivesProvided(), "onOrAfter=${startDate},onOrBefore=${endDate}"));
		}
		{
			CohortIndicator ind = createCohortIndicator("condomsProvided", "Provided with condoms");
			ind.setCohortDefinition(map(pwpCohorts.condomsProvided(), "onOrAfter=${startDate},onOrBefore=${endDate}"));
		}
	}

	/**
	 * Setup non-cohort SQL indicators
	 */
	private void setupNonCohortIndicators() {
		nonCohortIndicators = new HashMap<String, Indicator>();
		{
			HivCareVisitsIndicator ind = new HivCareVisitsIndicator();
			ind.addParameter(new Parameter("startDate", "Start Date", Date.class));
			ind.addParameter(new Parameter("endDate", "End Date", Date.class));
			ind.setFilter(HivCareVisitsIndicator.Filter.FEMALES_18_AND_OVER);
			nonCohortIndicators.put("hivCareVisitsFemale18", ind);
		}
		{
			HivCareVisitsIndicator ind = new HivCareVisitsIndicator();
			ind.addParameter(new Parameter("startDate", "Start Date", Date.class));
			ind.addParameter(new Parameter("endDate", "End Date", Date.class));
			ind.setFilter(HivCareVisitsIndicator.Filter.SCHEDULED);
			nonCohortIndicators.put("hivCareVisitsScheduled", ind);
		}
		{
			HivCareVisitsIndicator ind = new HivCareVisitsIndicator();
			ind.addParameter(new Parameter("startDate", "Start Date", Date.class));
			ind.addParameter(new Parameter("endDate", "End Date", Date.class));
			ind.setFilter(HivCareVisitsIndicator.Filter.UNSCHEDULED);
			nonCohortIndicators.put("hivCareVisitsUnscheduled", ind);
		}
		{
			HivCareVisitsIndicator ind = new HivCareVisitsIndicator();
			ind.addParameter(new Parameter("startDate", "Start Date", Date.class));
			ind.addParameter(new Parameter("endDate", "End Date", Date.class));
			nonCohortIndicators.put("hivCareVisitsTotal", ind);
		}
	}

	/**
	 * Creates the report data set
	 * @return the data set
	 */
	private DataSetDefinition createDataSet() {
		CohortIndicatorDataSetDefinition cohortDsd = new CohortIndicatorDataSetDefinition();
		cohortDsd.setName(getName() + " Cohort DSD");
		cohortDsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cohortDsd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cohortDsd.addDimension("age", map(commonDimensions.age(), "onDate=${endDate}"));
		cohortDsd.addDimension("gender", map(commonDimensions.gender()));

		SimpleIndicatorDataSetDefinition nonCohortDsd = new SimpleIndicatorDataSetDefinition();
		nonCohortDsd.setName(getName() + " Non-cohort DSD");
		nonCohortDsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		nonCohortDsd.addParameter(new Parameter("endDate", "End Date", Date.class));

		MergingDataSetDefinition mergedDsd = new MergingDataSetDefinition();
		mergedDsd.setName(getName() + " DSD");
		mergedDsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		mergedDsd.addParameter(new Parameter("endDate", "End Date", Date.class));
		mergedDsd.addDataSetDefinition(cohortDsd);
		mergedDsd.addDataSetDefinition(nonCohortDsd);
		mergedDsd.setMergeOrder(MergingDataSetDefinition.MergeOrder.NAME);

		/////////////// 3.1 (On Cotrimoxazole Prophylaxis) ///////////////

		// TODO

		/////////////// 3.2 (Enrolled in Care) ///////////////

		cohortDsd.addColumn("HV03-08", "Enrolled in care (<1)", map(artIndicators.enrolledExcludingTransfers(), "startDate=${startDate},endDate=${endDate}"), "age=<1");
		cohortDsd.addColumn("HV03-09", "Enrolled in care (<15, Male)", map(artIndicators.enrolledExcludingTransfers(), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=<15");
		cohortDsd.addColumn("HV03-10", "Enrolled in care (<15, Female)", map(artIndicators.enrolledExcludingTransfers(), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=<15");
		cohortDsd.addColumn("HV03-11", "Enrolled in care (15+, Male)", map(artIndicators.enrolledExcludingTransfers(), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=15+");
		cohortDsd.addColumn("HV03-12", "Enrolled in care (15+, Female)", map(artIndicators.enrolledExcludingTransfers(), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=15+");
		cohortDsd.addColumn("HV03-13", "Enrolled in care (Total)", map(artIndicators.enrolledExcludingTransfers(), "startDate=${startDate},endDate=${endDate}"), "");

		/////////////// 3.3 (Currently in Care) ///////////////

		cohortDsd.addColumn("HV03-14", "Currently in care (<1)", map(cohortIndicators.get("currentlyInCare"), "startDate=${startDate},endDate=${endDate}"), "age=<1");
		cohortDsd.addColumn("HV03-15", "Currently in care (<15, Male)", map(cohortIndicators.get("currentlyInCare"), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=<15");
		cohortDsd.addColumn("HV03-16", "Currently in care (<15, Female)", map(cohortIndicators.get("currentlyInCare"), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=<15");
		cohortDsd.addColumn("HV03-17", "Currently in care (15+, Male)", map(cohortIndicators.get("currentlyInCare"), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=15+");
		cohortDsd.addColumn("HV03-18", "Currently in care (15+, Female)", map(cohortIndicators.get("currentlyInCare"), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=15+");
		cohortDsd.addColumn("HV03-19", "Currently in care (Total)", map(cohortIndicators.get("currentlyInCare"), "startDate=${startDate},endDate=${endDate}"), "");

		/////////////// 3.4 (Starting ART) ///////////////

		cohortDsd.addColumn("HV03-20", "Starting ART (<1)", map(artIndicators.startedArt(), "startDate=${startDate},endDate=${endDate}"), "age=<1");
		cohortDsd.addColumn("HV03-21", "Starting ART (<15, Male)", map(artIndicators.startedArt(), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=<15");
		cohortDsd.addColumn("HV03-22", "Starting ART (<15, Female)", map(artIndicators.startedArt(), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=<15");
		cohortDsd.addColumn("HV03-23", "Starting ART (15+, Male)", map(artIndicators.startedArt(), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=15+");
		cohortDsd.addColumn("HV03-24", "Starting ART (15+, Female)", map(artIndicators.startedArt(), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=15+");
		cohortDsd.addColumn("HV03-25", "Starting ART (Total)", map(artIndicators.startedArt(), "startDate=${startDate},endDate=${endDate}"), "");
		cohortDsd.addColumn("HV03-26", "Starting ART (Pregnant)", map(artIndicators.startedArtWhilePregnant(), "startDate=${startDate},endDate=${endDate}"), "");
		cohortDsd.addColumn("HV03-27", "Starting ART (TB Patient)", map(cohortIndicators.get("startingArtTbPatient"), "startDate=${startDate},endDate=${endDate}"), "");

		/////////////// 3.5 (Revisits ART) ///////////////

		cohortDsd.addColumn("HV03-28", "Revisits ART (<1)", map(cohortIndicators.get("revisitsArt"), "startDate=${startDate},endDate=${endDate}"), "age=<1");
		cohortDsd.addColumn("HV03-29", "Revisits ART (<15, Male)", map(cohortIndicators.get("revisitsArt"), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=<15");
		cohortDsd.addColumn("HV03-30", "Revisits ART (<15, Female)", map(cohortIndicators.get("revisitsArt"), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=<15");
		cohortDsd.addColumn("HV03-31", "Revisits ART (15+, Male)", map(cohortIndicators.get("revisitsArt"), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=15+");
		cohortDsd.addColumn("HV03-32", "Revisits ART (15+, Female)", map(cohortIndicators.get("revisitsArt"), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=15+");
		cohortDsd.addColumn("HV03-33", "Revisits ART (Total)", map(cohortIndicators.get("revisitsArt"), "startDate=${startDate},endDate=${endDate}"), "");

		/////////////// 3.6 (Currently on ART [All]) ///////////////

		cohortDsd.addColumn("HV03-28", "Currently on ART [All] (<1)", map(cohortIndicators.get("currentlyOnArt"), "startDate=${startDate},endDate=${endDate}"), "age=<1");
		cohortDsd.addColumn("HV03-35", "Currently on ART [All] (<15, Male)", map(cohortIndicators.get("currentlyOnArt"), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=<15");
		cohortDsd.addColumn("HV03-36", "Currently on ART [All] (<15, Female)", map(cohortIndicators.get("currentlyOnArt"), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=<15");
		cohortDsd.addColumn("HV03-37", "Currently on ART [All] (15+, Male)", map(cohortIndicators.get("currentlyOnArt"), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=15+");
		cohortDsd.addColumn("HV03-38", "Currently on ART [All] (15+, Female)", map(cohortIndicators.get("currentlyOnArt"), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=15+");
		cohortDsd.addColumn("HV03-39", "Currently on ART [All] (Total)", map(cohortIndicators.get("currentlyOnArt"), "startDate=${startDate},endDate=${endDate}"), "");

		/////////////// 3.7 (Cumulative Ever on ART) ///////////////

		cohortDsd.addColumn("HV03-40", "Cumulative ever on ART (<15, Male)", map(cohortIndicators.get("cumulativeOnArt"), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=<15");
		cohortDsd.addColumn("HV03-41", "Cumulative ever on ART (<15, Female)", map(cohortIndicators.get("cumulativeOnArt"), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=<15");
		cohortDsd.addColumn("HV03-42", "Cumulative ever on ART (15+, Male)", map(cohortIndicators.get("cumulativeOnArt"), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=15+");
		cohortDsd.addColumn("HV03-43", "Cumulative ever on ART (15+, Female)", map(cohortIndicators.get("cumulativeOnArt"), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=15+");
		cohortDsd.addColumn("HV03-44", "Cumulative ever on ART (Total)", map(cohortIndicators.get("cumulativeOnArt"), "startDate=${startDate},endDate=${endDate}"), "");

		/////////////// 3.8 (Survival and Retention on ART at 12 months) ///////////////

		cohortDsd.addColumn("HV03-46", "ART Net Cohort at 12 months", map(cohortIndicators.get("art12MonthNetCohort"), "startDate=${startDate},endDate=${endDate}"), "");
		cohortDsd.addColumn("HV03-46", "On original 1st Line at 12 months", map(cohortIndicators.get("onOriginalFirstLineAt12Months"), "startDate=${startDate},endDate=${endDate}"), "");
		cohortDsd.addColumn("HV03-47", "On alternative 1st Line at 12 months", map(cohortIndicators.get("onAlternateFirstLineAt12Months"), "startDate=${startDate},endDate=${endDate}"), "");
		cohortDsd.addColumn("HV03-48", "On 2nd Line (or higher) at 12 months ", map(cohortIndicators.get("onSecondLineAt12Months"), "startDate=${startDate},endDate=${endDate}"), "");
		cohortDsd.addColumn("HV03-49", "On therapy at 12 months (Total) ", map(cohortIndicators.get("onTherapyAt12Months"), "startDate=${startDate},endDate=${endDate}"), "");

		/////////////// 3.9 (Screening) ///////////////

		cohortDsd.addColumn("HV03-50", "Screened for TB (<15, Male)", map(cohortIndicators.get("screenedForTb"), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=<15");
		cohortDsd.addColumn("HV03-51", "Screened for TB (<15, Female)", map(cohortIndicators.get("screenedForTb"), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=<15");
		cohortDsd.addColumn("HV03-52", "Screened for TB (15+, Male)", map(cohortIndicators.get("screenedForTb"), "startDate=${startDate},endDate=${endDate}"), "gender=M|age=15+");
		cohortDsd.addColumn("HV03-53", "Screened for TB (15+, Female)", map(cohortIndicators.get("screenedForTb"), "startDate=${startDate},endDate=${endDate}"), "gender=F|age=15+");
		cohortDsd.addColumn("HV03-54", "Screened for TB (Total)", map(cohortIndicators.get("screenedForTb"), "startDate=${startDate},endDate=${endDate}"), "");

		// TODO HV03-55 (Screened for cervical cancer (F 18+))

		/////////////// 3.10 (Prevention with Positives) ///////////////

		cohortDsd.addColumn("HV09-04", "Modern contraceptive methods", map(cohortIndicators.get("modernContraceptivesProvided"), "startDate=${startDate},endDate=${endDate}"), "");
		cohortDsd.addColumn("HV09-05", "Provided with condoms", map(cohortIndicators.get("condomsProvided"), "startDate=${startDate},endDate=${endDate}"), "");

		/////////////// 3.11 (HIV Care Visits) ///////////////

		nonCohortDsd.addColumn("HV03-70", "HIV care visits (Females 18+)", map(nonCohortIndicators.get("hivCareVisitsFemale18"), "startDate=${startDate},endDate=${endDate}"));
		nonCohortDsd.addColumn("HV03-71", "HIV care visits (Scheduled)", map(nonCohortIndicators.get("hivCareVisitsScheduled"), "startDate=${startDate},endDate=${endDate}"));
		nonCohortDsd.addColumn("HV03-72", "HIV care visits (Unscheduled)", map(nonCohortIndicators.get("hivCareVisitsUnscheduled"), "startDate=${startDate},endDate=${endDate}"));
		nonCohortDsd.addColumn("HV03-73", "HIV care visits (Total)", map(nonCohortIndicators.get("hivCareVisitsTotal"), "startDate=${startDate},endDate=${endDate}"));

		return mergedDsd;
	}

	protected CohortIndicator createCohortIndicator(String id, String description) {
		CohortIndicator ind = new CohortIndicator(description);
		ind.addParameter(new Parameter("startDate", "Start Date", Date.class));
		ind.addParameter(new Parameter("endDate", "End Date", Date.class));
		cohortIndicators.put(id, ind);
		return ind;
	}
}