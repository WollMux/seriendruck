package de.muenchen.mailmerge.dialog.mailmerge.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import de.muenchen.allg.itd51.wollmux.core.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.mailmerge.dialog.PrintParametersDialog;
import de.muenchen.mailmerge.dialog.mailmerge.MailMergeParams;

public class PrinterSettingsUIElement extends UIElement
{
  JTextField printerNameField;

  public PrinterSettingsUIElement(String label, String group,
      final MailMergeParams mmp)
  {
    super(Box.createHorizontalBox(), group);

    Box hbox = (Box) getCompo();
    printerNameField = new JTextField();
    printerNameField.setEditable(false);
    printerNameField.setFocusable(false);
    hbox.add(new JLabel(label));
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(printerNameField);
    hbox.add(Box.createHorizontalStrut(5));

    hbox.add(new JButton(new AbstractAction(L.m("Drucker wechseln"))
    {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent e)
      {
        mmp.getDruckerController().erzeugeView();
        printerNameField.setText(mmp.getDruckerController().getDrucker());
        PrintParametersDialog.setCurrentPrinterName(mmp.getMMC().getTextDocument(), mmp.getDruckerController().getDrucker());
       }
    }));

    hbox.add(Box.createHorizontalStrut(6));
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);

    printerNameField.setText(mmp.getDruckerController().getDrucker());
  }
}