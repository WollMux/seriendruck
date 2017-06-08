package de.muenchen.mailmerge.dialog.mailmerge.gui;

import java.util.Map;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import de.muenchen.allg.itd51.wollmux.core.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.mailmerge.dialog.mailmerge.MailMergeParams;

/**
 * Beschreibt das {@link UIElement} vom Typ {@link UIElementType#emailtofieldname}.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class EMailToFieldNameUIElement extends UIElement
{
  private final JComboBox<String> toFieldName;

  public EMailToFieldNameUIElement(String label, UIElementAction action,
      final String value, String group, final MailMergeParams mmp)
  {
    super(Box.createVerticalBox(), group);
    Box vbox = (Box) getCompo();

    Box hbox = Box.createHorizontalBox();
    hbox.add(new JLabel(label));
    hbox.add(Box.createHorizontalStrut(5));
    String[] fnames = new String[mmp.getMMC().getColumnNames().size() + 1];
    fnames[0] = L.m("-- Bitte auswählen --");
    int i = 1;
    int mailIdx = 0;
    for (String fname : mmp.getMMC().getColumnNames())
    {
      if (fname.toLowerCase().contains("mail") && mailIdx == 0)
        mailIdx = i;
      fnames[i++] = "<" + fname + ">";
    }
    this.toFieldName = new JComboBox<>(fnames);
    toFieldName.setSelectedIndex(mailIdx);
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(toFieldName);
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);

    vbox.add(hbox);
    vbox.add(Box.createVerticalStrut(5));
  }

  @Override
  public void addSubmitArgs(Map<SubmitArgument, Object> args)
      throws InvalidArgumentException
  {
    if (toFieldName.getSelectedIndex() == 0)
      throw new InvalidArgumentException(
        L.m("Sie müssen ein Feld angeben, das die Empfängeradresse enthält!"));

    String to = toFieldName.getSelectedItem().toString();
    to = to.substring(1, to.length() - 1);
    args.put(SubmitArgument.emailToFieldName, to);
  }
}