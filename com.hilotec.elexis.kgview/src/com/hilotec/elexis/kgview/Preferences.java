package com.hilotec.elexis.kgview;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;
import ch.elexis.preferences.inputs.MultilineFieldEditor;

/**
 * Einstelllungsseite fuer kgview-Plugin.
 * 
 * @author Antoine Kaufmann
 */
public class Preferences extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {
	public static final String CFG_EVLISTE = "hilotec/kgview/einnahmevorschriften";
	public static final String CFG_FLORDZ = "hilotec/kgview/ordnungszahlfavliste";
	public static final String CFG_AKG_HEARTBEAT = "hilotec/kgview/archivkgheartbaeat";
	public static final String CFG_AKG_SCROLLPERIOD = "hilotec/kgview/archivkgscrollperiod";
	public static final String CFG_AKG_SCROLLDIST_UP = "hilotec/kgview/archivkgscrolldistup";
	public static final String CFG_AKG_SCROLLDIST_DOWN = "hilotec/kgview/archivkgscrolldistdown";

	public Preferences() {
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(Hub.mandantCfg));
	}
	
	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		addField(new MultilineFieldEditor(CFG_EVLISTE, "Einnahmevorschriften",
				5, SWT.V_SCROLL, true, getFieldEditorParent()));
		addField(new BooleanFieldEditor(CFG_FLORDZ,
			    "Ordnungszahl in FML anzeigen", getFieldEditorParent()));
		addField(new IntegerFieldEditor(CFG_AKG_HEARTBEAT,
				"Archiv KG Heartbeat", getFieldEditorParent()));
		addField(new IntegerFieldEditor(CFG_AKG_SCROLLPERIOD,
				"Archiv KG Scroll Periode [ms]", getFieldEditorParent()));
		addField(new IntegerFieldEditor(CFG_AKG_SCROLLDIST_UP,
				"Archiv KG Scroll Distanz hoch [px]", getFieldEditorParent()));
		addField(new IntegerFieldEditor(CFG_AKG_SCROLLDIST_DOWN,
				"Archiv KG Scroll Distanz runter [px]", getFieldEditorParent()));
	}

	/**
	 * @return Konfigurierte Einnahmevorschriften im aktuellen Mandant.
	 */
	public static String[] getEinnahmevorschriften() {
		String s = Hub.mandantCfg.get(CFG_EVLISTE, "");
		return s.split(",");
	}
	
	/**
	 * @return
	 */
	public static boolean getOrdnungszahlInFML() {
		boolean oz = Hub.mandantCfg.get(CFG_FLORDZ, false);
		return oz;
	}
	
	/**
	 * @return Heartbeat abstand in Sekunden, fuer die Aktualisierung der
	 *         ArchivKG-Ansicht.
	 */
	public int getArchivKGHeartbeat() {
		int n = Integer.parseInt(Hub.mandantCfg.get(CFG_AKG_HEARTBEAT, "10"));
		if (n < 1) n = 1;
		return n;
	}
	
	/**
	 * @return Fuer automatisches Scrollen in ArchivKG, Periode in Millisekunden.
	 */
	public static int getArchivKGScrollPeriod() {
		int n = Integer.parseInt(Hub.mandantCfg.get(CFG_AKG_SCROLLPERIOD, "200"));
		if (n < 50) n = 50;
		return n;
	}
	
	/**
	 * @return Fuer automatisches Scrollen in ArchivKG, Scrolldistanz in Pixel
	 */
	public static int getArchivKGScrollDistUp() {
		int n = Integer.parseInt(Hub.mandantCfg.get(CFG_AKG_SCROLLDIST_UP, "5"));
		if (n < 1) n = 1;
		return n;
	}
	
	/**
	 * @return Fuer automatisches Scrollen in ArchivKG, Scrolldistanz in Pixel
	 */
	public static int getArchivKGScrollDistDown() {
		int n = Integer.parseInt(Hub.mandantCfg.get(CFG_AKG_SCROLLDIST_DOWN, "5"));
		if (n < 1) n = 1;
		return n;
	}
}
