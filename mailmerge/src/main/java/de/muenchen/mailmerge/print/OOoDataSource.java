package de.muenchen.mailmerge.print;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.sdb.XDocumentDataSource;

import de.muenchen.allg.itd51.wollmux.core.document.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.core.document.FormFieldFactory.FormFieldType;
import de.muenchen.allg.itd51.wollmux.core.document.SimulationResults;
import de.muenchen.allg.itd51.wollmux.core.document.SimulationResults.SimulationResultsProcessor;

/**
 * Repräsentiert eine (noch nicht registrierte) Datenquelle für OpenOffice.org.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public abstract class OOoDataSource implements SimulationResultsProcessor
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OOoDataSource.class);

  /**
   * Liefert das für die Registrierung der OOo-Datenquelle benötigte
   * {@link XDocumentDataSource}-Objekt zurück.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public abstract XDocumentDataSource createXDocumentDatasource();

  /**
   * Liefert einen {@link DataSourceWriter} zurück, über den Datensätze in die
   * Datenquelle geschrieben werden können.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public abstract DataSourceWriter getDataSourceWriter();

  /**
   * Liefert die Anzahl der Datensätze der Datenquelle zurück.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public abstract int getSize();

  /**
   * Entfernt die Datenquelle
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public abstract void remove();

  /*
   * (non-Javadoc)
   *
   * @see
   * de.muenchen.allg.itd51.wollmux.SimulationResults.SimulationResultsProcessor
   * #processSimulationResults(de.muenchen.allg.itd51.wollmux.SimulationResults)
   */
  @Override
  public void processSimulationResults(SimulationResults simRes)
  {
    if (simRes == null)
      return;

    HashMap<String, String> data = new HashMap<>(simRes.getFormFieldValues());
    for (FormField field : simRes.getFormFields())
    {
      String columnName = OOoBasedMailMerge.getSpecialColumnNameForFormField(field);
      if (columnName == null)
        continue;
      String content = simRes.getFormFieldContent(field);

      // Checkboxen müssen über bestimmte Zeichen der Schriftart OpenSymbol
      // angenähert werden.
      if (field.getType() == FormFieldType.CHECKBOX_FORM_FIELD)
        if (content.equalsIgnoreCase("TRUE"))
          content = "" + OOoBasedMailMerge.OPENSYMBOL_CHECKED;
        else
          content = "" + OOoBasedMailMerge.OPENSYMBOL_UNCHECKED;

      data.put(columnName, content);
    }

    for (Map.Entry<String, Boolean> entry : simRes.getGroupsVisibilityState().entrySet())
    {
      LOGGER.info("{} --> {}", entry.getKey(), entry.getValue());
      data.put(OOoBasedMailMerge.COLUMN_PREFIX_TEXTSECTION + entry.getKey(), entry.getValue().toString());
    }

    try
    {
      getDataSourceWriter().addDataset(data);
    }
    catch (Exception e)
    {
      LOGGER.error("", e);
    }
  }
}