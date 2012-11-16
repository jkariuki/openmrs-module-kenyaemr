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

import org.openmrs.module.kenyaemr.util.KenyaEmrUtils;
import org.openmrs.ui.framework.FormatterImpl;

import java.util.Date;

/**
 * UI utility methods for web pages
 */
public class KenyaEmrUiUtils {

	/**
	 * Formats a date ignoring any time information
	 * @param date the date
	 * @return the string value
	 * @should format date as a string without time information
	 */
	public static String formatDateNoTime(Date date) {
		Date dateOnly = KenyaEmrUtils.dateOnly(date);
		return new FormatterImpl().format(dateOnly);
	}
}
