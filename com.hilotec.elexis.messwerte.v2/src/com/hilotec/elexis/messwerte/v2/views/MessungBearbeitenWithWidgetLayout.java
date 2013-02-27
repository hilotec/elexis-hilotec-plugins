/*******************************************************************************
 * Copyright (c) 2011, Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    P. Chaubert - initial implementation
 *    
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.v2.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
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
import com.hilotec.elexis.messwerte.v2.data.MessungTyp;
import com.hilotec.elexis.messwerte.v2.data.Messwert;
import com.hilotec.elexis.messwerte.v2.data.Panel;
import com.hilotec.elexis.messwerte.v2.data.typen.IMesswertTyp;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypCalc;
import com.tiff.common.ui.datepicker.DatePickerCombo;

/**
 * Dialog um eine Messung zu bearbeiten oder neu zu erstellen
 * 
 * @author Patrick Chaubert
 */
public class MessungBearbeitenWithWidgetLayout extends TitleAreaDialog {
	private Messung messung;
	private List<Messwert> messwerte;
	private List<Messwert> shownMesswerte;
	private List<Messwert> calcFields;
	private DatePickerCombo dateWidget;
	private Text zeit;
	private String tabtitle;
	
	private Listener listener = new Listener() {
		public void handleEvent(Event event){
			Boolean validValues = true;
			for (Messwert mw : shownMesswerte) {
				IMesswertTyp typ = mw.getTyp();
				if (!typ.checkInput(mw, typ.getValidpattern())) {
					setErrorMessage(typ.getTitle() + ": " + typ.getInvalidmessage());
					validValues = false;
					System.out.println("Invalid values");
					break;
				}
			}
			if (validValues) {
				setErrorMessage(null);
				for (Messwert mw : calcFields) {
					System.out.println("Calc new values");
					((MesswertTypCalc) mw.getTyp()).calcNewValue(mw);
				}
			}
		}
	};
	
	public MessungBearbeitenWithWidgetLayout(final Shell parent, Messung m){
		super(parent);
		messung = m;
		shownMesswerte = new ArrayList<Messwert>();
		calcFields = new ArrayList<Messwert>();
	}
	
	public MessungBearbeitenWithWidgetLayout(Shell shell, Messung m, String text){
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
		dateWidget.setDate(new TimeTool(messung.getDatum()).getTime());
		dateWidget.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));

		new Label(comp, SWT.NONE).setText(Messages.MessungenUebersicht_23);
		zeit = SWTHelper.createText(comp, 1, SWT.WRAP);
		zeit.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		String t = messung.getZeit();
		if (!t.isEmpty())
			t = t.substring(0, 5);
		zeit.setText(t);

		messwerte = messung.getMesswerte();
		MessungTyp typ = messung.getTyp();
		
		createComposite(typ.getPanel(), comp);
		
		comp.setSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return scroll;
	}
	
	private Composite createComposite(Panel p, Composite parent){
		Composite c = new Composite(parent, SWT.NONE);
		if (p.getType().equals("plain")) {
			c.setLayout(new FillLayout());
		} else if (p.getType().equals("display")) {
			c.setLayout(new GridLayout());
			Browser browser = new Browser(c, SWT.NONE);
			String url = p.getAttribute("url");
			browser.setUrl(url == null ? "" : url);
			GridData gd = SWTHelper.getFillGridData(1, true, 1, true);
			String bounds = p.getAttribute("size");
			if (bounds != null) {
				String[] coord = bounds.trim().split("\\s*,\\s*");
				if (coord.length == 2) {
					gd = new GridData(Integer.parseInt(coord[0]), Integer.parseInt(coord[1]));
					gd.grabExcessHorizontalSpace = true;
					gd.grabExcessVerticalSpace = true;
				}
			}
			browser.setLayoutData(gd);
		} else if (p.getType().equals("label")) {
			c.setLayout(new FillLayout());
			Label l = new Label(c, SWT.WRAP);
			l.setText(p.getAttribute("text"));
		} else if (p.getType().equals("grid")) {
			String cols = p.getAttribute("columns");
			if (cols == null) {
				c.setLayout(new GridLayout());
			} else {
				c.setLayout(new GridLayout(Integer.parseInt(cols), false));
			}
			
		} else if (p.getType().equals("field")) {
			String fieldref = p.getAttribute("ref");
			Messwert mw = getMesswert(fieldref);
			if (mw != null) {
				IMesswertTyp dft = mw.getTyp();
				
				boolean bEditable = true;
				String attr = p.getAttribute("editable");
				if (attr != null && attr.equals("false")) {
					bEditable = false;
				}
				dft.setEditable(bEditable);
				
				String validpattern = p.getAttribute("validpattern");
				if (validpattern == null) {
					validpattern = "[\u0000-\uFFFF]*";
				}
				dft.setValidpattern(validpattern);
				String invalidMsg = p.getAttribute("invalidmessage");
				if (invalidMsg == null) {
					invalidMsg = "ungültige Eingabe";
				}
				dft.setInvalidmessage(invalidMsg);
				
				c.setLayout(new GridLayout());
				
				Label l = new Label(c, SWT.NONE);
				String labelText = dft.getTitle();
				if (!dft.getUnit().equals("")) {
					labelText += " [" + dft.getUnit() + "]";
				}
				l.setText(labelText);
				l.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
				Widget w = dft.createWidget(c, mw);
				
				if (dft instanceof MesswertTypCalc) {
					calcFields.add(mw);
				} else {
					w.addListener(SWT.Selection, listener);
					w.addListener(SWT.Modify, listener);
				}
				shownMesswerte.add(mw);
				setLayoutData(c);
			}
		}
		for (Panel panel : p.getPanels()) {
			setLayoutData(createComposite(panel, c));
		}
		return c;
	}
	
	private void setLayoutData(Composite c){
		if (c.getParent().getLayout() instanceof GridLayout) {
			c.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		}
		c.pack();
	}
	
	public Messwert getMesswert(String name){
		for (Messwert m : messwerte) {
			if (m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}
	
	@Override
	public void create(){
		super.create();
		getShell().setText("Messung bearbeiten");
		setTitle(messung.getTyp().getTitle());
	}
	
	@Override
	protected Control createContents(Composite parent){
		Control contents = super.createContents(parent);
		setTitle(tabtitle);
		return contents;
	}
	
	@Override
	public void okPressed(){
		boolean validValues = true;
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
		for (Messwert mw : shownMesswerte) {
			IMesswertTyp typ = mw.getTyp();
			if (!typ.checkInput(mw, typ.getValidpattern())) {
				setErrorMessage(typ.getTitle() + ": " + typ.getInvalidmessage());
				validValues = false;
				break;
			} else {
				typ.saveInput(mw);
			}
		}
		if (validValues) {
			close();
		}
	}
	
	@Override
	public boolean close(){
		for (Messwert mw : shownMesswerte) {
			mw.getTyp().setShown(false);
		}
		return super.close();
	}
}
