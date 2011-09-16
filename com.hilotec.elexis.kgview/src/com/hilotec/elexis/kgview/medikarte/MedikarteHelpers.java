package com.hilotec.elexis.kgview.medikarte;

import java.util.List;

import ch.elexis.data.Patient;
import ch.elexis.data.Prescription;
import ch.elexis.data.Query;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class MedikarteHelpers {
	
	/**
	 * Medikation auf der Medikarte des Patienten zusammensuchen.
	 * Wenn !alle, dann wird nur die noch aktuelle Medikation zurueckgegeben.
	 */
	public static List<Prescription> medikarteMedikation(
			Patient patient, boolean alle)
	{
		Query<Prescription> qbe = new Query<Prescription>(Prescription.class);
		qbe.add(Prescription.PATIENT_ID, Query.EQUALS, patient.getId());
		qbe.add(Prescription.REZEPT_ID, StringTool.leer, null);
		if (!alle) {
			qbe.startGroup();
			String today = new TimeTool().toString(TimeTool.DATE_COMPACT);
			qbe.add(Prescription.DATE_UNTIL, Query.GREATER_OR_EQUAL, today);
			qbe.or();
			qbe.add(Prescription.DATE_UNTIL, StringTool.leer, null);
			qbe.or();
			qbe.add(Prescription.DATE_UNTIL, Query.EQUALS, "");
			qbe.endGroup();
		}
		qbe.orderBy(true, Prescription.DATE_FROM, Prescription.DATE_UNTIL,
				Prescription.ARTICLE);
		return qbe.execute();
	}
	
	/**
	 * Datum der letzten Aenderung der Medikarte (letztes von oder bis datum)
	 */
	public static String medikarteDatum(Patient patient)
	{
		// TODO: Koennte man mit einer Query sauberer loesen
		List<Prescription> medis = medikarteMedikation(patient, false);
		TimeTool max = new TimeTool(0);
		TimeTool cur = new TimeTool();
		for (Prescription p: medis) {
			cur.set(p.getBeginDate());
			if (cur.isAfter(max)) max.set(p.getBeginDate());
			cur.set(p.getEndDate());
			if (cur.isAfter(max)) max.set(p.getEndDate());
		}
		
		return max.toString(TimeTool.DATE_GER);
	}
	
}
