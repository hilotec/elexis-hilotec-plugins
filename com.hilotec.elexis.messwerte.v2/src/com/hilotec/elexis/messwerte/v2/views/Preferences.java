/*******************************************************************************
 * Copyright (c) 2011, Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    P. Chaubert - adapted to Messwerte V2
 *    
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.v2.views;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;
import ch.elexis.preferences.inputs.InexistingFileOKFileFieldEditor;

public class Preferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public static final String CONFIG_FILE = "findings/hilotec/configfile";
	public static final String CONFIG_PATINFO = "findings/hilotec/patientinfo";
	public static final String CONFIG_ORDERDESC = "findings/hilotec/orderdesc";
	
	public void init(IWorkbench workbench){
		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
		getPreferenceStore().setDefault(CONFIG_PATINFO, true);
		getPreferenceStore().setDefault(CONFIG_ORDERDESC, false);
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new InexistingFileOKFileFieldEditor(CONFIG_FILE, "Konfigurationsdatei",
			getFieldEditorParent()));
		addField(new BooleanFieldEditor(CONFIG_PATINFO, "Patienteninfo anzeigen",
			getFieldEditorParent()));
		addField(new BooleanFieldEditor(CONFIG_ORDERDESC, "Absteigend sortieren",
			getFieldEditorParent()));
	}
	
	@Override
	public void performApply(){
		Hub.localCfg.flush();
	}
	
}
