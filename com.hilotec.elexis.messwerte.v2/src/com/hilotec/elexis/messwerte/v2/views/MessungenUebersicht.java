/*******************************************************************************
 * Copyright (c) 2009-2010, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    A. Kaufmann - copied from befunde-Plugin and adapted to new data structure 
 *    G. Weirich - adapted to Eventhandling API Change in 2.1
 *    M. Descher - added copy function for value, added CSV Export for a Table
 *    P. Chaubert - adapted to Messwerte V2
 *    
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.v2.views;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListener;
import ch.elexis.data.Patient;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.rgw.tools.TimeTool;

import com.hilotec.elexis.messwerte.v2.data.Messung;
import com.hilotec.elexis.messwerte.v2.data.MessungKonfiguration;
import com.hilotec.elexis.messwerte.v2.data.MessungTyp;
import com.hilotec.elexis.messwerte.v2.data.Messwert;
import com.hilotec.elexis.messwerte.v2.data.typen.IMesswertTyp;

/**
 * View fuer Uebersicht ueber alle Messungen des aktuellen Patienten
 * 
 * @author Antoine Kaufmann
 * 
 */
public class MessungenUebersicht extends ViewPart implements ElexisEventListener {
	private final boolean showPatientInfo;
	private MessungKonfiguration config;
	private ScrolledForm form;
	private ArrayList<MessungstypSeite> seiten;
	private CTabFolder tabsfolder;
	private boolean orderDesc;
	
	private Action neuAktion;
	private Action editAktion;
	private Action copyAktion;
	private Action loeschenAktion;
	private Action exportAktion;
	private Action reloadXMLAction;
	
	public MessungenUebersicht(){
		config = MessungKonfiguration.getInstance();
		seiten = new ArrayList<MessungstypSeite>();
		showPatientInfo = Hub.localCfg.get(Preferences.CONFIG_PATINFO, true);
		orderDesc = Hub.localCfg.get(Preferences.CONFIG_ORDERDESC, false);
	}
	
	/**
	 * Ein einzelner Tab fuer einen bestimmten Messungstyp. In diesem wird dann eine Tabelle mit
	 * allen Messungen des aktuell ausgewaehlten Patienten angezeigt.
	 * 
	 * @author Antoine Kaufmann
	 * 
	 */
	class MessungstypSeite extends Composite {
		private MessungTyp typ;
		private Table table;
		private TableColumn cols[];
		private Patient patient;
		
		/**
		 * Einzelnes Tab fuer einen bestimmten Typ Messungen mit Tabelle der Messwerte.
		 * 
		 * @param dt
		 *            Typ der Messungen
		 */
		public MessungstypSeite(Composite parent, MessungTyp dt){
			super(parent, SWT.NONE);
			typ = dt;
			
			parent.setLayout(new FillLayout());
			setLayout(new GridLayout());
			
			table = new Table(this, SWT.FULL_SELECTION | SWT.V_SCROLL);
			table.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
			table.setHeaderVisible(true);
			table.setLinesVisible(true);
			
			cols = new TableColumn[typ.getMesswertTypen().size() + 2];

			SelectionListener sortListener = new SelectionListener() {
				public void widgetSelected(SelectionEvent arg0) {
					orderDesc = !orderDesc;
					aktualisieren();
				}
				public void widgetDefaultSelected(SelectionEvent arg0) {}
			};

			// Spalten anlegen
			int i = 0;
			cols[i] = new TableColumn(table, SWT.NONE);
			cols[i].setText(Messages.MessungenUebersicht_0);
			cols[i].setWidth(80);
			cols[i].addSelectionListener(sortListener);
			i++;

			cols[i] = new TableColumn(table, SWT.NONE);
			cols[i].setText(Messages.MessungenUebersicht_23);
			cols[i].setWidth(40);
			cols[i].addSelectionListener(sortListener);
			i++;

			for (IMesswertTyp dft : typ.getMesswertTypen()) {
				cols[i] = new TableColumn(table, SWT.NONE);
				if (dft.getUnit().equals("")) { //$NON-NLS-1$
					cols[i].setText(dft.getTitle());
				} else {
					cols[i].setText(dft.getTitle() + " [" + dft.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				cols[i].setWidth(80);
				i++;
			}
			
			table.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDoubleClick(final MouseEvent e){
					editAktion.run();
				}
			});
			
			ViewMenus menu = new ViewMenus(getViewSite());
			menu.createControlContextMenu(table, editAktion, copyAktion, loeschenAktion, neuAktion,
				exportAktion);
		}
		
		/**
		 * Seite neu zeichnen (damit veraenterte Daten aktualisiert werden)
		 */
		public void aktualisieren(){
			table.removeAll();
			
			if (patient == null) {
				return;
			}
			
			for (Messung messung : Messung.getPatientMessungen(patient, typ, !orderDesc)) {
				TableItem ti = new TableItem(table, SWT.NONE);
				ti.setData(messung);
				
				int i = 0;
				ti.setText(i++, messung.getDatum());

				String t = messung.getZeit();
				t = t.isEmpty() ? t : t.substring(0, 5);
				ti.setText(i++, t);

				for (Messwert mwrt : messung.getMesswerte()) {
					ti.setText(i++, mwrt.getDarstellungswert());
				}
			}
		}
		
		/**
		 * Aktuell angezeigten Patienten festlegen. Dabei wird die Ansicht neu aufgebaut, da die
		 * Daten meist aendern.
		 */
		public void setCurPatient(Patient p){
			patient = p;
			aktualisieren();
		}
		
		/**
		 * @return Messungstyp der von dieser Seite angezeigt wird
		 */
		public MessungTyp getTyp(){
			return typ;
		}
	}
	
	/**
	 * Aktionen fuer Menuleiste und Kontextmenu initialisieren
	 */
	private void erstelleAktionen(){
		neuAktion = new Action(Messages.MessungenUebersicht_1) {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_ADDITEM));
				setToolTipText(Messages.MessungenUebersicht_2);
			}
			
			public void run(){
				Patient p = ElexisEventDispatcher.getSelectedPatient();
				if (p == null) {
					return;
				}
				
				CTabItem tab = tabsfolder.getSelection();
				MessungstypSeite mts = (MessungstypSeite) tab.getControl();
				Messung messung = new Messung(p, mts.getTyp());
				if (messung.getTyp().getPanel() == null) {
					MessungBearbeiten dialog =
						new MessungBearbeiten(getSite().getShell(), messung, tabsfolder
							.getSelection().getText());
					if (dialog.open() != Dialog.OK) {
						messung.delete();
					}
					aktualisieren();
				} else {
					MessungBearbeitenWithWidgetLayout dialog =
						new MessungBearbeitenWithWidgetLayout(getSite().getShell(), messung);
					if (dialog.open() != Dialog.OK) {
						messung.delete();
					}
					aktualisieren();
				}
				
			}
		};
		
		editAktion = new Action(Messages.MessungenUebersicht_3) {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_EDIT));
				setToolTipText(Messages.MessungenUebersicht_4);
			}
			
			public void run(){
				CTabItem ci = tabsfolder.getSelection();
				if (ci == null) {
					return;
				}
				MessungstypSeite seite = (MessungstypSeite) ci.getControl();
				TableItem[] tableitems = seite.table.getSelection();
				if (tableitems.length == 1) {
					Messung messung = (Messung) tableitems[0].getData();
					if (messung.getTyp().getPanel() == null) {
						MessungBearbeiten dialog =
							new MessungBearbeiten(getSite().getShell(), messung, ci.getText());
						if (dialog.open() == Dialog.OK) {
							aktualisieren();
							
						}
					} else {
						MessungBearbeitenWithWidgetLayout dialog =
							new MessungBearbeitenWithWidgetLayout(getSite().getShell(), messung);
						dialog.open();
						aktualisieren();
					}
				}
			}
		};
		
		copyAktion = new Action(Messages.MessungenUebersicht_5) {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_CLIPBOARD));
				setToolTipText(Messages.MessungenUebersicht_6);
			}
			
			public void run(){
				CTabItem ci = tabsfolder.getSelection();
				if (ci == null) {
					return;
				}
				
				MessungstypSeite seite = (MessungstypSeite) ci.getControl();
				TableItem[] tableitems = seite.table.getSelection();
				if (tableitems.length == 1) {
					Messung messung = (Messung) tableitems[0].getData();
					String messungsdatum = messung.getDatum();
					TimeTool date = new TimeTool();
					String newdatum = date.toString(TimeTool.DATE_GER);
					
					if (!messungsdatum.equalsIgnoreCase(newdatum)) {
						// Nur wenn Messung nich vom selben Tag wie heute!!
						Messung messungnew = new Messung(messung.getPatient(), messung.getTyp());
						messungnew.setDatum(date.toString(TimeTool.DATE_GER));
						
						for (Messwert messwert : messung.getMesswerte()) {
							Messwert copytemp = messungnew.getMesswert(messwert.getName());
							copytemp.setWert(messwert.getWert());
						}
						
						aktualisieren();
						
					} else {
						SWTHelper.showError(Messages.MessungenUebersicht_7,
							Messages.MessungenUebersicht_8);
					}
					
				}
				
			}
		};
		
		exportAktion = new Action(Messages.MessungenUebersicht_9) {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_EXPORT));
				setToolTipText(Messages.MessungenUebersicht_10);
			}
			
			public void run(){
				CTabItem ci = tabsfolder.getSelection();
				if (ci == null) {
					return;
				}
				
				MessungstypSeite seite = (MessungstypSeite) ci.getControl();
				TableItem[] tableitems = seite.table.getItems();
				
				try {
					String date = new TimeTool().toString(TimeTool.DATE_COMPACT);
					String filename = ci.getText() + "-export-" + date + ".csv"; //$NON-NLS-1$ //$NON-NLS-2$
					String fqfilename =
						System.getProperty("user.home") + File.separatorChar + filename; //$NON-NLS-1$
					FileWriter writer = new FileWriter(fqfilename);
					
					// Get the headers Name (Unit); Name (Unit); ...
					Messung headermessung = (Messung) tableitems[0].getData();
					String headerstring = "datum;"; //$NON-NLS-1$
					for (Messwert messwert : headermessung.getMesswerte()) {
						headerstring =
							headerstring + messwert.getTyp().getName() + "(" //$NON-NLS-1$
								+ messwert.getTyp().getUnit() + ")" + ";"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					headerstring = headerstring.substring(0, headerstring.length() - 1);
					writer.append(headerstring + "\n"); //$NON-NLS-1$
					
					for (int i = 0; i < tableitems.length; i++) {
						Messung messung = (Messung) tableitems[i].getData();
						String messungstring = messung.getDatum() + ";"; //$NON-NLS-1$
						for (Messwert messwert : messung.getMesswerte()) {
							messungstring =
								messungstring
									+ messwert.getTyp().erstelleDarstellungswert(messwert) + ";"; //$NON-NLS-1$
						}
						messungstring = messungstring.substring(0, messungstring.length() - 1);
						writer.append(messungstring + "\n"); //$NON-NLS-1$
					}
					
					writer.flush();
					writer.close();
					
					SWTHelper.showInfo(Messages.MessungenUebersicht_11 + ci.getText() + Messages.MessungenUebersicht_12,
						Messages.MessungenUebersicht_13 + ci.getText() + Messages.MessungenUebersicht_14
							+ fqfilename);
				} catch (Exception e) {
					SWTHelper.showError(Messages.MessungenUebersicht_15, e.toString());
				}
				
			}
		};
		
		loeschenAktion = new Action(Messages.MessungenUebersicht_16) {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_DELETE));
				setToolTipText(Messages.MessungenUebersicht_17);
			}
			
			public void run(){
				CTabItem ci = tabsfolder.getSelection();
				if (ci == null) {
					return;
				}
				
				MessungstypSeite seite = (MessungstypSeite) ci.getControl();
				TableItem[] tableitems = seite.table.getSelection();
				if ((tableitems.length > 0)
					&& SWTHelper.askYesNo(Messages.MessungenUebersicht_18,
						Messages.MessungenUebersicht_19)) {
					for (TableItem ti : tableitems) {
						Messung messung = (Messung) ti.getData();
						messung.delete();
					}
					aktualisieren();
				}
			}
		};
		
		reloadXMLAction = new Action(Messages.MessungenUebersicht_20) {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_REFRESH));
				setToolTipText(Messages.MessungenUebersicht_21);
			}
			
			public void run(){
				for (CTabItem ci : tabsfolder.getItems()) {
					ci.getControl().dispose();
					ci.dispose();
				}
				for (Control c : tabsfolder.getChildren()) {
					c.dispose();
				}
				config.readFromXML(null);
				erstelleSeiten();
				aktualisieren();
			}
		};
	}
	
	/**
	 * Menuleiste generieren
	 */
	private ViewMenus erstelleMenu(IViewSite site){
		ViewMenus menu = new ViewMenus(site);
		erstelleAktionen();
		menu.createToolbar(neuAktion, editAktion, copyAktion, loeschenAktion, exportAktion);
		menu.createMenu(reloadXMLAction);
		return menu;
	}
	
	@Override
	public void createPartControl(Composite parent){
		parent.setLayout(new GridLayout());
		
		form = Desk.getToolkit().createScrolledForm(parent);
		form.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		Composite body = form.getBody();
		body.setLayout(new FillLayout());
		tabsfolder = new CTabFolder(body, SWT.NONE);
		tabsfolder.setLayout(new FillLayout());
		
		erstelleMenu(getViewSite());
		erstelleSeiten();
		ElexisEventDispatcher.getInstance().addListeners(this);
	}
	
	void erstelleSeiten(){
		for (MessungTyp t : config.getTypes()) {
			CTabItem cti = new CTabItem(tabsfolder, SWT.NONE);
			cti.setText(t.getTitle());
			MessungstypSeite mts = new MessungstypSeite(tabsfolder, t);
			seiten.add(mts);
			cti.setControl(mts);
		}
		tabsfolder.setSelection(0);
		setCurPatient(ElexisEventDispatcher.getSelectedPatient());
	}
	
	@Override
	public void dispose(){
		ElexisEventDispatcher.getInstance().removeListeners(this);
	}
	
	@Override
	public void setFocus(){
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Aktuell ausgewaehlten Patient festlegen
	 * 
	 * @param patient
	 *            Ausgewaehlter Patient oder null falls keiner ausgewaehlt ist.
	 */
	private void setCurPatient(Patient patient){
		// Patienteninfo anzeigen, falls gewuenscht
		if (showPatientInfo) {
			if (patient == null) {
				form.setText(Messages.MessungenUebersicht_22);
			} else {
				form.setText(patient.getLabel());
			}
		}
		
		// Tabs benachrichtigen
		for (MessungstypSeite mts : seiten) {
			mts.setCurPatient(patient);
		}
	}
	
	/**
	 * Alle Seiten aktualisieren
	 */
	private void aktualisieren(){
		for (MessungstypSeite mts : seiten) {
			mts.aktualisieren();
		}
	}
	
	/**
	 * Dieser Event-Handler ist dafuer zustaendig, uns ueber den aktuell ausgewaehlten Patienten auf
	 * dem Laufenden zu halten, damit die Ansicht aktualisiert wird.
	 */
	
	public void catchElexisEvent(final ElexisEvent ev){
		Desk.asyncExec(new Runnable() {
			
			public void run(){
				if (ev.getType() == ElexisEvent.EVENT_SELECTED) {
					setCurPatient((Patient) ev.getObject());
				} else if (ev.getType() == ElexisEvent.EVENT_DESELECTED) {
					setCurPatient(null);
					
				}
			}
		});
	}
	
	private final ElexisEvent eetmpl = new ElexisEvent(null, Patient.class,
		ElexisEvent.EVENT_SELECTED | ElexisEvent.EVENT_DESELECTED);
	
	public ElexisEvent getElexisEventFilter(){
		return eetmpl;
	}
}
