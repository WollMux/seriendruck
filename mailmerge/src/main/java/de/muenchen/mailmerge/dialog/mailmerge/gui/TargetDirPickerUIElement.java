package de.muenchen.mailmerge.dialog.mailmerge.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;

import de.muenchen.allg.itd51.wollmux.core.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.mailmerge.dialog.mailmerge.MailMergeParams;

/**
 * Beschreibt das {@link UIElement} vom Typ
 * {@link UIElementType#filenametemplatechooser}.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class TargetDirPickerUIElement extends UIElement
{
  private final JTextField targetDirectory;

  public TargetDirPickerUIElement(String label, UIElementAction action,
      final String value, String group, final MailMergeParams mmp)
  {
    super(Box.createHorizontalBox(), group);
    Box hbox = (Box) getCompo();
    this.targetDirectory = new JTextField();
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(targetDirectory);
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(targetDirectory);
    hbox.add(new JButton(new AbstractAction(label)
    {
      private static final long serialVersionUID = -7919862309134895087L;

      public void actionPerformed(ActionEvent e)
      {
        final JFileChooser fc = new JFileChooser(targetDirectory.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setDialogTitle(L.m("Verzeichnis für die Serienbriefdateien wählen"));
        int ret = fc.showSaveDialog(mmp.getDialog());
        if (ret == JFileChooser.APPROVE_OPTION)
          targetDirectory.setText(fc.getSelectedFile().getAbsolutePath());
      }
    }));
    hbox.add(Box.createHorizontalStrut(6));
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);
  }

  public void addSubmitArgs(Map<SubmitArgument, Object> args)
      throws InvalidArgumentException
  {
    String dir = targetDirectory.getText();
    if (dir.length() == 0)
      throw new InvalidArgumentException(
        L.m("Sie müssen ein Zielverzeichnis angeben!"));
    File targetDirFile = new File(dir);
    if (!targetDirFile.isDirectory())
      throw new InvalidArgumentException(L.m(
        "%1\n existiert nicht oder ist kein Verzeichnis!", dir));

    args.put(SubmitArgument.targetDirectory, dir);
  }
}