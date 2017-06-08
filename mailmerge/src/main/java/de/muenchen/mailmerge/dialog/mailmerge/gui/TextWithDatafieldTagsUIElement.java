package de.muenchen.mailmerge.dialog.mailmerge.gui;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.wollmux.core.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.core.dialog.JPotentiallyOverlongPopupMenuButton;
import de.muenchen.allg.itd51.wollmux.core.dialog.TextComponentTags;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.mailmerge.dialog.mailmerge.MailMergeParams;

/**
 * Beschreibt ein UIElement, das {@link JTextComponent}s über
 * {@link TextComponentTags} editieren kann und derzeit für die Elemente vom Typ
 * {@link UIElementType#emailtext} und
 * {@link UIElementType#filenametemplatechooser} verwendet wird.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class TextWithDatafieldTagsUIElement extends UIElement
{
  private TextComponentTags textTags;

  private String errorMessageIfEmpty;

  private SubmitArgument argKey;

  public TextWithDatafieldTagsUIElement(JTextComponent textCompo,
      JComponent toAdd, SubmitArgument argKey, String errorMessageIfEmpty,
      String group, final MailMergeParams mmp)
  {
    super(Box.createVerticalBox(), group);
    Box vbox = (Box) getCompo();

    this.setTextTags(new TextComponentTags(textCompo));

    Box hbox = Box.createHorizontalBox();
    hbox.add(Box.createHorizontalGlue());
    JPotentiallyOverlongPopupMenuButton insertFieldButton =
      new JPotentiallyOverlongPopupMenuButton(L.m("Serienbrieffeld"),
        TextComponentTags.makeInsertFieldActions(mmp.getMMC().getColumnNames(),
          getTextTags()));
    hbox.add(insertFieldButton);
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(new JPotentiallyOverlongPopupMenuButton(L.m("Spezialfeld"),
      makeSpecialFieldActions(getTextTags())));
    hbox.add(Box.createHorizontalStrut(6));
    vbox.add(hbox);
    vbox.add(Box.createVerticalStrut(3));

    hbox = Box.createHorizontalBox();
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(toAdd);
    hbox.add(Box.createHorizontalStrut(5));
    vbox.add(hbox);
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(vbox);

    this.errorMessageIfEmpty = errorMessageIfEmpty;
    this.argKey = argKey;
  }

  public TextComponentTags getTextTags()
  {
    return textTags;
  }

  public void setTextTags(TextComponentTags textTags)
  {
    this.textTags = textTags;
  }

  @Override
  public void addSubmitArgs(Map<SubmitArgument, Object> args)
      throws InvalidArgumentException
  {
    if (errorMessageIfEmpty != null && getTextTags().getContent().isEmpty())
      throw new InvalidArgumentException(errorMessageIfEmpty);
    args.put(argKey, getTextTags());
  }

  /**
   * Erzeugt eine Liste von Actions zum Einfügen von Spezialfeld-Tags in tags.
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  private List<Action> makeSpecialFieldActions(final TextComponentTags tags)
  {
    List<Action> actions = new Vector<>();
    actions.add(new AbstractAction(L.m("Datensatznummer"))
    {
      private static final long serialVersionUID = 2675809156807460816L;

      @Override
      public void actionPerformed(ActionEvent e)
      {
        tags.insertTag(MailMergeParams.TAG_DATENSATZNUMMER);
      }
    });
    actions.add(new AbstractAction(L.m("Serienbriefnummer"))
    {
      private static final long serialVersionUID = 3779132684393223573L;

      @Override
      public void actionPerformed(ActionEvent e)
      {
        tags.insertTag(MailMergeParams.TAG_SERIENBRIEFNUMMER);
      }
    });
    return actions;
  }
}