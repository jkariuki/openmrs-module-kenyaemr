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

package org.openmrs.module.kenyaemr.api.impl;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.*;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.*;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.test.TestUtils;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link KenyaEmrServiceImpl}
 */
public class KenyaEmrServiceImplTest extends BaseModuleContextSensitiveTest {

	@Autowired
	private KenyaEmrService service;

	@Autowired
	private KenyaEmr emr;

	@Before
	public void setup() throws Exception {
		executeDataSet("test-data.xml");

		emr.getMetadataManager().setupGlobalProperties();
	}

	/**
	 * @see KenyaEmrServiceImpl#getDefaultLocation()
	 * @verifies throw an exception if the default location has not been set
	 */
	@Test(expected = APIException.class)
	public void getDefaultLocation_shouldThrowAnExceptionIfTheDefaultLocationHasNotBeenSet() throws Exception {
		Assert.assertTrue(StringUtils.isEmpty(Context.getAdministrationService().getGlobalProperty(KenyaEmrConstants.GP_DEFAULT_LOCATION)));
		service.getDefaultLocation();
	}

	/**
	 * @see KenyaEmrServiceImpl#getDefaultLocation()
	 * @verifies get the default location when set
	 */
	@Test
	public void getDefaultLocation_shouldGetTheDefaultLocationWhenSet() throws Exception {
		// setup data
		Location loc = Context.getLocationService().getLocation(1);
		Assert.assertNotNull(loc);
		service.setDefaultLocation(loc);

		// test
		Location defaultLocation = service.getDefaultLocation();
		Assert.assertEquals(loc, defaultLocation);
	}

	/**
	 * @see KenyaEmrServiceImpl#getLocationByMflCode(String)
	 * @verifies find the location with that code
	 * @verifies return null if no location has that code
	 */
	@Test
	public void getLocationByMflCode_shouldFindLocationWithCodeOrNull() throws Exception {
		Assert.assertEquals(Context.getLocationService().getLocation(1), service.getLocationByMflCode("15001"));
		Assert.assertNull(service.getLocationByMflCode("15003")); // Location is retired
		Assert.assertNull(service.getLocationByMflCode("XXXXX")); // No such MFL code
	}

	/**
	 * @see KenyaEmrServiceImpl#isConfigured()
	 * @verifies return false before default location has been set
	 */
	@Test
	public void isConfigured_shouldReturnFalseBeforeDefaultLocationHasBeenSet() throws Exception {
		Assert.assertFalse(service.isConfigured());
	}

	/**
	 * @see KenyaEmrServiceImpl#isConfigured()
	 * @verifies return true after everything is configured
	 */
	@Test
	public void isConfigured_shouldReturnTrueAfterEverythingIsConfigured() throws Exception {
		Assert.assertFalse(service.isConfigured());

		// default location
		Location loc = Context.getLocationService().getLocation(1);
		Assert.assertNotNull(loc);
		service.setDefaultLocation(loc);

		// MRN ID source
		emr.getIdentifierManager().setupMrnIdentifierSource(null);

		// HIV ID source
		emr.getIdentifierManager().setupHivUniqueIdentifierSource(null);

		Assert.assertTrue(service.isConfigured());
	}

	/**
	 * @see KenyaEmrServiceImpl#getVisitsByPatientAndDay(org.openmrs.Patient, java.util.Date)
	 */
	@Test
	public void getVisitsByPatientAndDay_shouldGetVisitsOnDayWithPatient() {
		Patient patient = Context.getPatientService().getPatient(7);
		VisitType outpatientType = Metadata.getVisitType(Metadata.OUTPATIENT_VISIT_TYPE);

		// Save visit from 10-11am and another from 12 onwards (no end)
		Visit visit1 = TestUtils.saveVisit(patient, outpatientType, TestUtils.date(2012, 1, 1, 10, 0, 0), TestUtils.date(2012, 1, 1, 11, 0, 0));
		Visit visit2 = TestUtils.saveVisit(patient, outpatientType, TestUtils.date(2012, 1, 1, 12, 0, 0), null);

		// Check no visits on day before
		List<Visit> visits = service.getVisitsByPatientAndDay(patient, TestUtils.date(2011, 12, 31));
		Assert.assertEquals(0, visits.size());

		// Check two visits on that day
		visits = service.getVisitsByPatientAndDay(patient, TestUtils.date(2012, 1, 1));
		Assert.assertEquals(2, visits.size());
		Assert.assertEquals(visit1, visits.get(0));
		Assert.assertEquals(visit2, visits.get(1));

		// Check only the ongoing visit the next day
		visits = service.getVisitsByPatientAndDay(patient, TestUtils.date(2012, 1, 2));
		Assert.assertEquals(1, visits.size());
		Assert.assertEquals(visit2, visits.get(0));
	}

	/**
	 * @see KenyaEmrServiceImpl#getLocations(String, org.openmrs.Location, java.util.Map, boolean, Integer, Integer)
	 */
	@Test
	public void getLocations_shouldGetAllLocationsWithMatchingArguments() {
		LocationAttributeType mflCodeAttrType = Metadata.getLocationAttributeType(Metadata.MASTER_FACILITY_CODE_LOCATION_ATTRIBUTE_TYPE);

		// Search for location #1 by MFL code and don't include retired
		Map<LocationAttributeType, Object> attrValues = new HashMap<LocationAttributeType, Object>();
		attrValues.put(mflCodeAttrType, "15001");
		List<Location> locations = service.getLocations(null, null, attrValues, false, null, null);
		Assert.assertEquals(1, locations.size());
		Assert.assertEquals(new Integer(1), locations.get(0).getLocationId());

		// Search for location #3 by MFL code and don't include retired
		attrValues = new HashMap<LocationAttributeType, Object>();
		attrValues.put(mflCodeAttrType, "15003");
		locations = service.getLocations(null, null, attrValues, false, null, null);
		Assert.assertEquals(0, locations.size());

		// Search for location #3 by MFL code and do include retired
		attrValues = new HashMap<LocationAttributeType, Object>();
		attrValues.put(mflCodeAttrType, "15003");
		locations = service.getLocations(null, null, attrValues, true, null, null);
		Assert.assertEquals(1, locations.size());
		Assert.assertEquals(new Integer(3), locations.get(0).getLocationId());
	}

	/**
	 * @see KenyaEmrServiceImpl#getLocations(String, org.openmrs.Location, java.util.Map, boolean, Integer, Integer)
	 */
	@Test
	public void getLocations_shouldGetAllLocationsWithAllGivenAttributeValues() {
		LocationAttributeType mflCodeAttrType = Metadata.getLocationAttributeType(Metadata.MASTER_FACILITY_CODE_LOCATION_ATTRIBUTE_TYPE);

		// Save new phone number attribute type
		LocationAttributeType phoneAttrType = new LocationAttributeType();
		phoneAttrType.setName("Facility Phone");
		phoneAttrType.setMinOccurs(0);
		phoneAttrType.setMaxOccurs(1);
		phoneAttrType.setDatatypeClassname("org.openmrs.customdatatype.datatype.FreeTextDatatype");
		Context.getLocationService().saveLocationAttributeType(phoneAttrType);

		// Assign phone number 0123456789 to locations #1 and #2
		Location location1 = Context.getLocationService().getLocation(1);
		LocationAttribute la1 = new LocationAttribute();
		la1.setAttributeType(phoneAttrType);
		la1.setValue("0123456789");
		location1.addAttribute(la1);
		Context.getLocationService().saveLocation(location1);
		Location location2 = Context.getLocationService().getLocation(2);
		LocationAttribute la2 = new LocationAttribute();
		la2.setAttributeType(phoneAttrType);
		la2.setValue("0123456789");
		location2.addAttribute(la2);
		Context.getLocationService().saveLocation(location2);

		// Search for location #1 by MFL code AND phone number
		Map<LocationAttributeType, Object> attrValues = new HashMap<LocationAttributeType, Object>();
		attrValues.put(mflCodeAttrType, "15001");
		attrValues.put(phoneAttrType, "0123456789");

		// Check that only location #1 is returned
		List<Location> locations = service.getLocations(null, null, attrValues, false, null, null);
		Assert.assertEquals(1, locations.size());
		Assert.assertEquals(location1, locations.get(0));
	}

	/**
	 * @see KenyaEmrServiceImpl#getLocations(String, org.openmrs.Location, java.util.Map, boolean, Integer, Integer)
	 */
	@Test
	public void getLocations_shouldNotFindAnyLocationsIfNoneHaveGivenAttributeValues() {
		LocationAttributeType mflCodeAttrType = Metadata.getLocationAttributeType(Metadata.MASTER_FACILITY_CODE_LOCATION_ATTRIBUTE_TYPE);
		Map<LocationAttributeType, Object> attrValues = new HashMap<LocationAttributeType, Object>();
		attrValues.put(mflCodeAttrType, "xxxxxx");
		List<Location> locations = service.getLocations(null, null, attrValues, true, null, null);
		Assert.assertEquals(0, locations.size());
	}
}