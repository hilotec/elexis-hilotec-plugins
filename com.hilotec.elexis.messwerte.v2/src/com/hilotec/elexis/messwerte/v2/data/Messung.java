/*******************************************************************************
 * Copyright (c) 2009, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation 
 *    P. Chaubert - adapted to Messwerte V2
 *    
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.v2.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.hilotec.elexis.messwerte.v2.Activator;
import com.hilotec.elexis.messwerte.v2.data.typen.IMesswertTyp;

import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;

import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class Messung extends PersistentObject {
	private static final String VERSION = "3";
	public static final String PLUGIN_ID = Activator.PLUGIN_ID;
	private static final String TABLENAME = "COM_HILOTEC_ELEXIS_MESSWERTE_MESSUNGEN";
	
	static {
		addMapping(TABLENAME, "PatientID", "TypName", "Datum=S:D:Datum",
			"Zeit");
		checkTable();
	}
	
	private static final String create = "CREATE TABLE " + TABLENAME + " ("
		+ "  ID			VARCHAR(25) PRIMARY KEY, " + "  lastupdate 	BIGINT, "
		+ "  deleted		CHAR(1) DEFAULT '0', " + "  PatientID	VARCHAR(25), "
		+ "  TypName		VARCHAR(25), " + "  Datum		CHAR(8) "
		+ "  Zeit			TIME " + ");" + "INSERT INTO " + TABLENAME
		+ " (ID, TypName) VALUES " + "	('VERSION', '" + VERSION + "');";
	
	private static final String up_2to3 =
		"ALTER TABLE " + TABLENAME
			+ "  ADD Zeit  TIME AFTER Datum;"
			+ "UPDATE " + TABLENAME + " SET TypName = '3' WHERE"
			+ "  ID LIKE 'VERSION';";

	/**
	 * Pruefen ob die Tabelle existiert
	 */
	private static void checkTable(){
		Messung check = load("VERSION");
		if (!check.exists()) {
			createOrModifyTable(create);
		} else {
			String v = check.get("TypName");
			if (v.equals("1") || v.equals("2"))
				createOrModifyTable(up_2to3);
		}
	}
	
	@Override
	public String getLabel(){
		return get("TypName");
	}
	
	@Override
	public String getTableName(){
		return TABLENAME;
	}
	
	/**
	 * Dieser Konstruktor darf nicht oeffentlich sein
	 */
	protected Messung(){}
	
	/**
	 * Vorhandene Messung anhand der ID erstellen
	 * 
	 * @param id
	 */
	protected Messung(String id){
		super(id);
	}
	
	/**
	 * Neue Messung erstellen
	 * 
	 * @param patient
	 *            Patient, dem diese Messung zugeordnet werden soll
	 * @param typ
	 *            Typ der Messung
	 */
	public Messung(Patient patient, MessungTyp typ){
		create(null);
		set("PatientID", patient.getId());
		set("TypName", typ.getName());
		TimeTool tt = new TimeTool();
		set("Datum", tt.toString(TimeTool.DATE_GER));
		set("Zeit", tt.toString(TimeTool.TIME_FULL));
	}
	
	/**
	 * Messung anhand der ID laden
	 * 
	 * @param id
	 *            ID der Messung
	 * @return Messung
	 */
	public static Messung load(final String id){
		return new Messung(id);
	}
	
	/**
	 * Datum dieser Messung
	 */
	public String getDatum(){
		return get("Datum");
	}

	/**
	 * Uhrzeit dieser Messung
	 */
	public String getZeit(){
		return StringTool.unNull(get("Zeit"));
	}

	/**
	 * TimeTool fuer Datum und Zeit initialisieren
	 */
	public TimeTool getTimeTool(){
		TimeTool tt = new TimeTool(getDatum());
		String t = getZeit();
		if (!t.isEmpty())
			tt.setTime(new TimeTool(t));
		return tt;
	}

	/**
	 * Messwert in dieser Messung anhand seines Namens holen
	 * 
	 * @param name
	 *            Name des Messwerttyps
	 * @return Messwert
	 */
	public Messwert getMesswert(String name){
		return getMesswert(name, true);
	}
	
	public Messwert getMesswert(String name, boolean create){
		Query<Messwert> query = new Query<Messwert>(Messwert.class);
		query.add("MessungID", Query.EQUALS, getId());
		query.and();
		query.add("Name", Query.EQUALS, name);
		List<Messwert> list = query.execute();
		
		if (list.size() == 0) {
			if (create) {
				// Messwert existiert noch nicht, wir legen ihn neu an
				return new Messwert(this, name);
			}
			return null;
		}
		
		return list.get(0);
	}
	
	/**
	 * @return Liste aller Messwerte
	 */
	public List<Messwert> getMesswerte(){
		List<IMesswertTyp> fields = getTyp().getMesswertTypen();
		ArrayList<Messwert> messwerte = new ArrayList<Messwert>();
		
		for (IMesswertTyp dft : fields) {
			messwerte.add(getMesswert(dft.getName()));
		}
		
		return messwerte;
	}
	
	/**
	 * @param datum
	 *            Datum neu setzen
	 */
	public void setDatum(String datum){
		set("Datum", datum);
	}
	
	/**
	 * @param zeit
	 *            Uhrzeit der Messung neu setzen
	 */
	public void setZeit(String zeit){
		if (zeit.isEmpty())
			zeit = null;
		set("Zeit", zeit);
	}

	/**
	 * @return Typ der Messung
	 */
	public MessungTyp getTyp(){
		MessungKonfiguration config = MessungKonfiguration.getInstance();
		return config.getTypeByName(get("TypName"));
	}
	
	/**
	 * @return Patient zu dem diese Messung gehoert
	 */
	public Patient getPatient(){
		return Patient.load(get("PatientID"));
	}
	
	/**
	 * Alle Messungen eines bestimmten Typs zu einem bestimmten Patienten zusammensuchen.
	 * 
	 * @param patient
	 *            Der Patient
	 * @param typ
	 *            Typ der zu suchenden Messungen
	 * @param aufsteigend
	 *            Aufsteigend sortieren?
	 * @return Liste mit den Messungen
	 */
	public static List<Messung> getPatientMessungen(Patient patient, MessungTyp typ,
													final boolean aufsteigend)
	{
		Query<Messung> query = new Query<Messung>(Messung.class);
		query.add("PatientID", Query.EQUALS, patient.getId());
		if (typ != null) {
			query.and();
			query.add("TypName", Query.EQUALS, typ.getName());
		}
		List<Messung> result = query.execute();

		Comparator<Messung> comp = new Comparator<Messung>() {
			public int compare(Messung arg0, Messung arg1) {
				TimeTool tt0 = arg0.getTimeTool();
				TimeTool tt1 = arg1.getTimeTool();
				int res = tt0.compareTo(tt1);
				if (!aufsteigend) res *= -1;
				return res;
			}
		};
		Collections.sort(result, comp);
		return result;
	}
	
	/**
	 * Alle Messungen eines bestimmten Typs zusammensuchen.
	 * 
	 * @param typ
	 *            Typ der zu suchenden Messung
	 * 
	 * @return Liste mit den Messungen
	 */
	public static List<Messung> getMessungen(MessungTyp typ){
		Query<Messung> query = new Query<Messung>(Messung.class);
		query.add("TypName", Query.EQUALS, typ.getName());
		return query.execute();
	}
}
