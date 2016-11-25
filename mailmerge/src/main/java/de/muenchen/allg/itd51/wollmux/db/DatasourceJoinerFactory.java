package de.muenchen.allg.itd51.wollmux.db;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.db.AttachDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnTransformer;
import de.muenchen.allg.itd51.wollmux.core.db.Datasource;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.LDAPDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorage;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorageDummyImpl;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorageStandardImpl;
import de.muenchen.allg.itd51.wollmux.core.db.OOoDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.OverlayDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.PreferDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.SchemaDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.ThingyDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.UnionDatasource;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;

public class DatasourceJoinerFactory
{
  /**
   * Enthält den zentralen DataSourceJoiner.
   */
  private static DatasourceJoiner datasourceJoiner;

  /**
   * Initialisiert den DJ wenn nötig und liefert ihn dann zurück (oder null, falls
   * ein Fehler während der Initialisierung aufgetreten ist).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static DatasourceJoiner getDatasourceJoiner()
  {
    if (datasourceJoiner == null)
    {
      ConfigThingy senderSource =
        WollMuxFiles.getWollmuxConf().query("SENDER_SOURCE", 1);
      String senderSourceStr = null;
      try
      {
        senderSourceStr = senderSource.getLastChild().toString();
      }
      catch (NodeNotFoundException e)
      {
        // hier geben wir im Vergleich zu früher keine Fehlermeldung mehr aus,
        // sondern erst später, wnn
        // tatsächlich auf die Datenquelle "null" zurück gegriffen wird.
      }
  
      ConfigThingy dataSourceTimeout =
        WollMuxFiles.getWollmuxConf().query("DATASOURCE_TIMEOUT", 1);
      String datasourceTimeoutStr = "";
      long datasourceTimeoutLong = 0;
      try
      {
        datasourceTimeoutStr = dataSourceTimeout.getLastChild().toString();
        try
        {
          datasourceTimeoutLong = new Long(datasourceTimeoutStr).longValue();
        }
        catch (NumberFormatException e)
        {
          Logger.error(L.m("DATASOURCE_TIMEOUT muss eine ganze Zahl sein"));
          datasourceTimeoutLong = DatasourceJoiner.DATASOURCE_TIMEOUT;
        }
        if (datasourceTimeoutLong <= 0)
        {
          Logger.error(L.m("DATASOURCE_TIMEOUT muss größer als 0 sein!"));
        }
      }
      catch (NodeNotFoundException e)
      {
        datasourceTimeoutLong = DatasourceJoiner.DATASOURCE_TIMEOUT;
      }
  
      try
      {
        if (null == senderSourceStr)
          senderSourceStr = DatasourceJoiner.NOCONFIG;
  
        datasourceJoiner =
          new DatasourceJoiner(collectDatasources(WollMuxFiles.getWollmuxConf(),
              WollMuxFiles.getDEFAULT_CONTEXT()), 
              senderSourceStr, 
              createLocalOverrideStorage(senderSourceStr, WollMuxFiles.getLosCacheFile(), WollMuxFiles.getDEFAULT_CONTEXT()),
              datasourceTimeoutLong);
        /*
         * Zum Zeitpunkt wo der DJ initialisiert wird sind die Funktions- und
         * Dialogbibliothek des WollMuxSingleton noch nicht initialisiert, deswegen
         * können sie hier nicht verwendet werden. Man könnte die Reihenfolge
         * natürlich ändern, aber diese Reihenfolgeabhängigkeit gefällt mir nicht.
         * Besser wäre auch bei den Funktionen WollMuxSingleton.getFunctionDialogs()
         * und WollMuxSingleton.getGlobalFunctions() eine on-demand initialisierung
         * nach dem Prinzip if (... == null) initialisieren. Aber das heben wir uns
         * für einen Zeitpunkt auf, wo es benötigt wird und nehmen jetzt erst mal
         * leere Dummy-Bibliotheken.
         */
        FunctionLibrary funcLib = new FunctionLibrary();
        DialogLibrary dialogLib = new DialogLibrary();
        Map<Object, Object> context = new HashMap<Object, Object>();
        ColumnTransformer columnTransformer =
          new ColumnTransformer(FunctionFactory.parseTrafos(WollMuxFiles.getWollmuxConf(),
            "AbsenderdatenSpaltenumsetzung", funcLib, dialogLib, context));
        datasourceJoiner.setTransformer(columnTransformer);
      }
      catch (ConfigurationErrorException e)
      {
        Logger.error(e);
      }
    }
  
    return datasourceJoiner;
  }

  private static Map<String, Datasource> collectDatasources(ConfigThingy joinConf, URL context)
  {
    HashMap<String, Datasource> datasources = new HashMap<String, Datasource>();
    
    ConfigThingy datenquellen = joinConf.query("Datenquellen").query("Datenquelle");
    Iterator<ConfigThingy> iter = datenquellen.iterator();
    while (iter.hasNext())
    {
      ConfigThingy sourceDesc = iter.next();
      ConfigThingy c = sourceDesc.query("NAME");
      if (c.count() == 0)
      {
        Logger.error(L.m("Datenquelle ohne NAME gefunden"));
        continue;
      }
      String name = c.toString();

      c = sourceDesc.query("TYPE");
      if (c.count() == 0)
      {
        Logger.error(L.m("Datenquelle %1 hat keinen TYPE", name));
        continue;
      }
      String type = c.toString();

      Datasource ds = null;
      try
      {
        if (type.equals("conf"))
          ds = new ThingyDatasource(datasources, sourceDesc, context);
        else if (type.equals("union"))
          ds = new UnionDatasource(datasources, sourceDesc, context);
        else if (type.equals("attach"))
          ds = new AttachDatasource(datasources, sourceDesc, context);
        else if (type.equals("overlay"))
          ds = new OverlayDatasource(datasources, sourceDesc, context);
        else if (type.equals("prefer"))
          ds = new PreferDatasource(datasources, sourceDesc, context);
        else if (type.equals("schema"))
          ds = new SchemaDatasource(datasources, sourceDesc, context);
        else if (type.equals("ldap"))
          ds = new LDAPDatasource(datasources, sourceDesc, context);
        else if (type.equals("ooo"))
          ds = new OOoDatasource(datasources, sourceDesc, context);
        else if (type.equals("funky"))
          ds = new FunkyDatasource(datasources, sourceDesc, context);
        else
          Logger.error(L.m("Ununterstützter Datenquellentyp: %1", type));
      }
      catch (Exception x)
      {
        Logger.error(L.m(
          "Fehler beim Initialisieren von Datenquelle \"%1\" (Typ \"%2\"):", name,
          type), x);
      }

      if (ds == null)
      {
        Logger.error(L.m(
          "Datenquelle '%1' von Typ '%2' konnte nicht initialisiert werden", name,
          type));
        /*
         * Falls schon eine alte Datenquelle name registriert ist, entferne diese
         * Registrierung. Ansonsten würde mit der vorher registrierten Datenquelle
         * weitergearbeitet, was seltsame Effekte zur Folge hätte die schwierig
         * nachzuvollziehen sind.
         */
        datasources.put(name, null);
        continue;
      }

      datasources.put(name, ds);
    }
    
    return datasources;
  }
  
  private static LocalOverrideStorage createLocalOverrideStorage(String mainSourceName, File losCache, URL context)
  {
    // kann sein, dass noch kein singleton erstellt ist - kein Zugriff auf no config
    if (mainSourceName.equals(DatasourceJoiner.NOCONFIG))
    {
      return new LocalOverrideStorageDummyImpl();// no config, kein cache ! 
    }
    else
    {
      return new LocalOverrideStorageStandardImpl(losCache, context);//mit config
    }
  }
}
