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
 *    M. Descher - Override createContents to include Tab title
 *    G. Weirich - added layout options
 *    P. Chaubert - adapted to Messwerte V2
 *    
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.v2.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import ch.elexis.util.SWTHelper;
import ch.rgw.tools.TimeTool;

import com.hilotec.elexis.messwerte.v2.data.Messung;
import com.hilotec.elexis.messwerte.v2.data.Messwert;
import com.hilotec.elexis.messwerte.v2.data.typen.IMesswertTyp;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypCalc;
import com.tiff.common.ui.datepicker.DatePickerCombo;

/**
 * Dialog um eine Messung zu bearbeiten oder neu zu erstellen ohne spezielle
 * Layout-Einstellungen.
 * 
 * @author Antoine Kaufmann
 */
public class MessungBearbeiten extends TitleAreaDialog {
	private Messung messung;
	private DatePickerCombo dateWidget;
	private Text zeit;
	private String tabtitle;
	
	/** Liste der Calc-Fields damit sie aktualisiert werden koennen. */
	private List<Messwert> calcFields;
	
	public MessungBearbeiten(final Shell parent, Messung m){
		super(parent);
		messung = m;
	}
	
	public MessungBearbeiten(Shell shell, Messung m, String text){
		this(shell, m);
		tabtitle = text;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		ScrolledComposite scroll =
			new ScrolledComposite(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		Composite comp = new Composite(scroll, SWT.NONE);
		scroll.setContent(comp);
		
		comp.setLayout(new GridLayout());
		comp.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		
		dateWidget = new DatePickerCombo(comp, SWT.NONE);
		dateWidget.setDate(messung.getTimeTool().getTime());
		dateWidget.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		new Label(comp, SWT.NONE).setText(Messages.MessungenUebersicht_23);
		zeit = SWTHelper.createText(comp, 1, SWT.WRAP);
		zeit.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		String t = messung.getZeit();
		if (!t.isEmpty())
			t = t.substring(0, 5);
		zeit.setText(t);

		// Berechnete Felder aktualisieren wenn Eingabefelder veraendert werden
		Listener recalcListener = new Listener() {
			public void handleEvent(Event event) {
				for (Messwert mw: calcFields) {
					MesswertTypCalc mtc = (MesswertTypCalc) mw.getTyp();
					mtc.calcNewValue(mw);
				}
			}
		};
		
		calcFields = new ArrayList<Messwert>();
		for (Messwert messwert : messung.getMesswerte()) {
			Label l = new Label(comp, SWT.NONE);
			IMesswertTyp dft = messwert.getTyp();
			String labelText = dft.getTitle();
			if (!dft.getUnit().equals("")) {
				labelText += " [" + dft.getUnit() + "]";
			}
			l.setText(labelText);
			
			Widget w = dft.createWidget(comp, messwert);
			
			// Listener einrichten damit Calcfields aktualisiert werden koennen.
			if (dft instanceof MesswertTypCalc)
				calcFields.add(messwert);
			else
				w.addListener(SWT.Modify, recalcListener);
		}
		comp.setSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return scroll;
		
	}
	
	@Override
	public void create(){
		super.create();
		getShell().setText("Messung bearbeiten");
		
	}
	
	@Override
	protected Control createContents(Composite parent){
		Control contents = super.createContents(parent);
		setTitle(tabtitle);
		return contents;
	}
	
	@Override
	public void okPressed(){
		TimeTool tt = new TimeTool(dateWidget.getDate().getTime());

		String t = zeit.getText();
		if (!t.isEmpty() && !t.matches("[0-9]{2}:[0-9]{2}")) {
			setMessage("Ungueltige Zeit: Muss entweder leer oder im Format" +
				"HH:MM sein");
			return;
		} else if (!t.isEmpty()) {
			t += ":00";
		}

		messung.setZeit(t);
		messung.setDatum(tt.toString(TimeTool.DATE_GER));
		for (Messwert mw : messung.getMesswerte()) {
			mw.getTyp().saveInput(mw);
		}
		close();
	}
	
	@Override
	public boolean close(){
		for (Messwert mw : messung.getMesswerte()) {
			mw.getTyp().setShown(false);
		}
		return super.close();
	}
}
