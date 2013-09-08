package com.hilotec.elexis.kgview.patientenfelder;

import org.eclipse.swt.graphics.Image;

import ch.elexis.Desk;
import ch.elexis.data.Patient;

import com.hilotec.elexis.kgview.PatientTextFView;

public class AllergienView extends PatientTextFView {
	public static final String ID = "com.hilotec.elexis.kgview.AllergienView";
	private static final String DBFIELD = Patient.FLD_ALLERGIES;

	public AllergienView() {
		super(DBFIELD);
	}

	@Override
	protected void setEmpty() {
		super.setEmpty();
		Image warn = Desk.getImage(Desk.IMG_FEHLER);
		if (isEmpty())
			warn = null;
		setTitleImage(warn);
	}
}
