<%
	def onAddVisitSuccess = "location.href = ui.pageLink('kenyaemr', 'enterHtmlForm', { patientId: ${ patient.id }, formUuid: '${ MetadataConstants.MOH_257_VISIT_SUMMARY_FORM_UUID }', visitId: data.visitId, returnUrl: location.href })"
%>

<div class="ke-panel-frame">
	<div class="ke-panel-heading">Page 1 (Care Summary)</div>
	<div class="ke-panel-content" style="background-color: #F3F9FF">

		<fieldset>
			<legend>New Forms</legend>

			${ ui.includeFragment("kenyaui", "widget/formStack", [ visit: null, forms: page1AvailableForms ]) }
		</fieldset>
		<br />
		<fieldset>
			<legend>Previously Completed Forms</legend>
			<%
				if (page1Encounters && page1Encounters.size > 0) {
					page1Encounters.each {
						println ui.includeFragment("kenyaemr", "encounterStackItem", [ encounter: it ])
					}
				} else {
					println "<i>None</i>"
				}
			%>
		</fieldset>
	</div>
</div>

<div class="ke-panel-frame">
	<div class="ke-panel-heading">Page 2 (Initial and Followup Visits)</div>
	<div class="ke-panel-content" style="background-color: #F3F9FF">
		<%
			if (page2Encounters && page2Encounters.size > 0) {
				page2Encounters.each {
					println ui.includeFragment("kenyaemr", "encounterStackItem", [ encounter: it, showEncounterDate: true ])
				}
			} else {
				println "<i>None</i>"
			}
		%>
		<br />
		<div align="center">
			${ ui.includeFragment("kenyaui", "widget/popupForm", [
					id: "create-retro-visit",
					buttonConfig: [
							iconProvider: "kenyaui",
							icon: "buttons/visit_retrospective.png",
							label: "Add Visit Summary",
							extra: "From column",
							classes: [ "padded" ]
					],
					popupTitle: "Visit Summary Details",
					prefix: "visit",
					commandObject: newREVisit,
					hiddenProperties: [ "patientId" ],
					properties: [ "visitType", "location", "visitDate" ],
					propConfig: [
							"visitType": [ type: "radio" ],
					],
					fragmentProvider: "kenyaemr",
					fragment: "moh257",
					action: "createRetrospectiveVisit",
					successCallbacks: [ onAddVisitSuccess ],
					submitLabel: ui.message("general.submit"),
					cancelLabel: ui.message("general.cancel"),
					submitLoadingMessage: "Creating retrospective visit"
			]) }
		</div>
	</div>
</div>

<div class="ke-panel-frame">
	<div class="ke-panel-heading">ARV Regimen History</div>
	<div class="ke-panel-content" style="background-color: #F3F9FF">
		${ ui.includeFragment("kenyaemr", "regimenHistory", [ history: arvHistory ]) }

		<br />
		<div align="center">
			${ ui.includeFragment("kenyaui", "widget/button", [
					label: "Edit History",
					extra: "Go to editor",
					iconProvider: "kenyaui",
					icon: "buttons/regimen.png",
					classes: [ "padded" ],
					href: ui.pageLink("kenyaemr", "regimenEditor", [ patientId: patient, category: "ARV", returnUrl: ui.thisUrl() ])
			]) }
		</div>
	</div>
</div>