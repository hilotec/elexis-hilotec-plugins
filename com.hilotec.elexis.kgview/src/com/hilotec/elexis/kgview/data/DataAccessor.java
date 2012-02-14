package com.hilotec.elexis.kgview.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.hilotec.elexis.kgview.Preferences;
import com.hilotec.elexis.kgview.diagnoseliste.DiagnoselisteItem;
import com.hilotec.elexis.kgview.medikarte.MedikarteHelpers;

import ch.elexis.data.Artikel;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Prescription;
import ch.elexis.util.IDataAccess;
import ch.rgw.tools.Result;
import ch.rgw.tools.StringTool;

public class DataAccessor implements IDataAccess {
	private static final String[] fields = {
		"JetzigesLeiden", "JetzigesLeidenICPC",
		"AllgStatus", "LokStatus",
		"Prozedere", "ProzedereICPC",
		"Diagnose", "DiagnoseICPC"};

	public String getName() {
		return "Hilotec-KG";
	}

	public String getDescription() {
		return "Daten aus dem Hilotec-KG-Plugin";
	}

	public List<Element> getList() {
		ArrayList<Element> res = new ArrayList<Element>(fields.length);
		for (String s: fields) {
			res.add(new Element(IDataAccess.TYPE.STRING, s, s,
				Konsultation.class, 0));
		}
		return res;
	}

	private String[] emptyRow(int count) {
		String[] row = new String[count];
		for (int i = 0; i < row.length; i++)
			row[i] = "";
		return row;
	}
	
	private Result<Object> generiereMedikarte(Patient pat) {
		List<Prescription> l = MedikarteHelpers.
			medikarteMedikation(pat, false);
		
		// Liste aller Einnahmevorschriften und Medis gruppiert nach EV
		SortedSet<String> evs = new TreeSet<String>();
		HashMap<String, List<Prescription>> prescs =
			new HashMap<String, List<Prescription>>();
		for (Prescription p: l) {
			String ev = p.getBemerkung();
			evs.add(ev);
			if (!prescs.containsKey(ev))
				prescs.put(ev, new ArrayList<Prescription>());
			prescs.get(ev).add(p);
		}
		
		// Die einzelnen Listen nach Ordnungszahl sortieren
		for (String ev: evs) {
			List<Prescription> pl = prescs.get(ev);
			Collections.sort(pl, new Comparator<Prescription>() {
				@Override
				public int compare(Prescription p1, Prescription p2) {
					Integer o1 = MedikarteHelpers.getOrdnungszahl(p1);
					Integer o2 = MedikarteHelpers.getOrdnungszahl(p2);
					
					// - weil die Grössten oben sein sollen
					if (o1 != o2)
						return -o1.compareTo(o2);
					
					Artikel a1 = p1.getArtikel();
					Artikel a2 = p2.getArtikel();
					
					// Wenn beide die gleiche Ordnungszahl haben, dann
					// alphabetisch nach Fav-Medi Name
					FavMedikament fm1 = FavMedikament.load(a1);
					FavMedikament fm2 = FavMedikament.load(a2);
					if (fm1 != null && fm2 != null)
						return fm1.getBezeichnung().compareTo(
								fm2.getBezeichnung());
					
					// Als letzte Moeglichkeit nehmen wir das Artikel-Label
					return a1.getLabel().compareTo(a1.getLabel());
				}
			});
		}
		
		// Zeilen fuer Spaltenueberschriften zaehlen
		int extra = 2 * evs.size() - (evs.contains("") ? 2 : 1);
		String[][] res = new String[l.size() + extra][];
		
		
		// Einnahmevorschriften Sortieren
		String evs_ordered[] = new String[evs.size()];
		int i = 0;
		// Zuerst Medikamente ohne Vorschrift
		if (evs.contains("")) {
			evs_ordered[i++] = "";
			evs.remove("");
		}
		// Dann die konfigurierten Vorschriften in der konfigurierten
		// Reihenfolge
		String evs_config[] = Preferences.getEinnahmevorschriften();
		for (String ev: evs_config) {
			if (evs.contains(ev)) {
				evs_ordered[i++] = ev;
				evs.remove(ev);
			}
		}
		// Am schluss noch der Rest alphabetisch
		for (String ev: evs) {
			evs_ordered[i++] = ev;
			evs.remove(ev);
		}
		
		
		// Tabelle Generieren
		i = 0;
		for (String ev: evs_ordered) {
			// Ueberschrift
			if (i != 0) res[i++] = emptyRow(7);
			if (!ev.equals("")) {
				String[] row = emptyRow(7);
				row[0] = "_" + ev + "_";
				res[i++] = row;
			}
			
			// Medikamente
			for (Prescription p: prescs.get(ev)){
				FavMedikament fm = FavMedikament.load(p.getArtikel());
				String[] dosierung = p.getDosis().split("-");
				if (dosierung.length != 4 || fm == null) continue;
				
				String[] row = new String[7];
				row[0] = fm.getBezeichnung();
				row[5] = fm.getEinheit();
				row[6] = fm.getZweck();
				row[1] = dosierung[0];
				row[2] = dosierung[1];
				row[3] = dosierung[2];
				row[4] = dosierung[3];
				
				res[i++] = row;
			}
		}
		return new Result<Object>(res);
	}
	
	/** Tiefe des Baums, nur Wurzel hat Tiefe 1. */
	private int dliDepth(DiagnoselisteItem item) {
		if (item == null) return 0;
		int depth = 0;
		for (DiagnoselisteItem child: item.getChildren()) {
			depth = Math.max(dliDepth(child), depth);
		}
		return depth + 1;
	}
	
	/**
	 * Generiere Zeile fuer ein DLI, und dessen Unteritems.
	 *
	 * @param item   Item fuer das eine Zeile generiert werden soll.
	 * @param skip   Anzahl der Spalten am Anfang zu ueberspringen
	 * @param ignore Nur fuer Unterelemente generieren.
	 * @param dest   Liste in der die Zeilen abgelegt werden sollen.
	 */
	private void generiereDLIZeilen(DiagnoselisteItem item, int skip,
			boolean ignore, List<String[]> dest)
	{
		if (!ignore) {
			skip++;
			String[] zeile = new String[skip];
			zeile[skip - 1] = item.getText();
			dest.add(zeile);
		}
		
		for (DiagnoselisteItem child: item.getChildren()) {
			generiereDLIZeilen(child, skip, false, dest);
		}
	}
	
	private Result<Object> generiereDiagnoseliste(Patient pat) {
		// TODO: Waere schoen wenn wir den Baum hier nicht 2x traversieren
		// muessten.
		DiagnoselisteItem root = DiagnoselisteItem.getRoot(pat);
		int depth = dliDepth(root);
		
		// Nur Wurzel oder weniger -> abbrechen.
		if (depth <= 1) {
			return new Result<Object>(""); 
		}
		
		List<String[]> rows = new ArrayList<String[]>();
		generiereDLIZeilen(root, 0, true, rows);
		
		// Resultat in Array umwandeln
		String[][] res = new String[rows.size()][];
		int i = 0;
		for (String[] row: rows) {
			res[i++] = row;
		}
		
		return new Result<Object>(res);
	}
	
	public Result<Object> getObject(String descriptor,
			PersistentObject dependentObject, String dates, String[] params)
	{
		if (dependentObject instanceof Patient &&
				descriptor.equals("Medikarte"))
		{
			return generiereMedikarte((Patient) dependentObject);
		} else if (dependentObject instanceof Patient &&
				descriptor.equals("Medikarte.Datum"))
		{
			return new Result<Object>(
				MedikarteHelpers.medikarteDatum((Patient) dependentObject));
		} else if (dependentObject instanceof Patient &&
				descriptor.equals("Diagnoseliste"))
		{
			return generiereDiagnoseliste((Patient) dependentObject);
		}
		if (!(dependentObject instanceof Konsultation)) {
			return new Result<Object>(Result.SEVERITY.ERROR,
				IDataAccess.INVALID_PARAMETERS, "Ungültiger Parameter",
				dependentObject, true);
		}
		Konsultation kons = (Konsultation) dependentObject;
		KonsData data = new KonsData(kons);
		
		for (String f: fields) {
			if (f.equals(descriptor)) {
				return new Result<Object>(StringTool.unNull(data.get(f)));
			}
		}
		return new Result<Object>(Result.SEVERITY.ERROR, IDataAccess.INVALID_PARAMETERS,
				"Ungültiges Feld", dependentObject, true);
	}

}
