package net.sourceforge.squirrel_sql.plugins.hibernate;

import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.fw.util.Utilities;
import net.sourceforge.squirrel_sql.plugins.hibernate.mapping.MappedClassInfo;
import net.sourceforge.squirrel_sql.plugins.hibernate.mapping.HibernatePropertyInfo;

import java.net.URLClassLoader;
import java.util.*;

public class HibernateConnection
{
   private static ILogger s_log = LoggerController.createLogger(HibernateConnection.class);
   private Object _sessionFactoryImpl;
   private URLClassLoader _cl;


   public HibernateConnection(Object sessionFactoryImpl, URLClassLoader cl)
   {
      _sessionFactoryImpl = sessionFactoryImpl;
      _cl = cl;
   }


   public ArrayList<String> generateSQL(String hqlQuery)
   {
      try
      {


         Class sessionFactoryImplementorClass = (Class) new ReflectionCaller().getClass("org.hibernate.engine.SessionFactoryImplementor", _cl).getCallee();

         List<ReflectionCaller> translators =
         new ReflectionCaller(_cl)
            .getClass("org.hibernate.engine.query.HQLQueryPlan", _cl)
            .callConstructor(new Class[]{String.class, Boolean.TYPE, Map.class, sessionFactoryImplementorClass}, new Object[]{hqlQuery, false, Collections.EMPTY_MAP, _sessionFactoryImpl})
            .callArrayMethod("getTranslators");

         ArrayList<String> ret = new ArrayList<String>();


         for (ReflectionCaller translator : translators)
         {
            List sqls = (List) translator.callMethod("collectSqlStrings").getCallee();

            for (Object sql : sqls)
            {
               ret.add(sql.toString());
            }
         }
         return ret;
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }



   public void close()
   {
      try
      {
         new ReflectionCaller(_sessionFactoryImpl).callMethod("close");
      }
      catch (Throwable t)
      {
         s_log.error(t);
      }
      _sessionFactoryImpl = null;
      _cl = null;
      System.gc();

   }

   public ArrayList<MappedClassInfo> getMappedClassInfos()
   {

      ArrayList<MappedClassInfo> ret = new ArrayList<MappedClassInfo>();

      ReflectionCaller sessionFactoryImplcaller = new ReflectionCaller(_sessionFactoryImpl);
      Collection<ReflectionCaller> persisters = sessionFactoryImplcaller.callMethod("getAllClassMetadata").callCollectionMethod("values");

      for (ReflectionCaller persister : persisters)
      {
         Object entityMode_POJO = persister.getClass("org.hibernate.EntityMode", _cl).getField("POJO").getCallee();
         Class mappedClass = (Class) persister.callMethod("getMappedClass", new Object[]{entityMode_POJO}).getCallee();

         String identifierPropertyName = (String) persister.callMethod("getIdentifierPropertyName").getCallee();

         Class identifierPropertyClass = persister.callMethod("getIdentifierType").callMethod("getReturnedClass").getCalleeClass();

         String identifierPropertyClassName = identifierPropertyClass.getName();

         HibernatePropertyInfo identifierPropInfo = new HibernatePropertyInfo(identifierPropertyName, identifierPropertyClassName, null);


         String[] propertyNames = (String[]) persister.callMethod("getPropertyNames").getCallee();

         HibernatePropertyInfo[] infos = new HibernatePropertyInfo[propertyNames.length];
         for (int i = 0; i < propertyNames.length; i++)
         {
            ReflectionCaller propertyTypeCaller = persister.callMethod("getPropertyType", new String[]{propertyNames[i]});
            String mayBeCollectionTypeName = propertyTypeCaller.callMethod("getReturnedClass").getCalleeClass().getName();

            try
            {
               // If this isn't instanceof org.hibernate.type.CollectionType a NoSuchMethodException will be thrown
               String role = (String) propertyTypeCaller.callMethod("getRole").getCallee();

               ReflectionCaller collectionMetaDataCaller = sessionFactoryImplcaller.callMethod("getCollectionMetadata", new Object[]{role});
               String typeName = collectionMetaDataCaller.callMethod("getElementType").callMethod("getReturnedClass").getCalleeClass().getName();

               infos[i] = new HibernatePropertyInfo(propertyNames[i], typeName, mayBeCollectionTypeName);
            }
            catch(RuntimeException e)
            {
               if(Utilities.getDeepestThrowable(e) instanceof NoSuchMethodException)
               {
                  infos[i] = new HibernatePropertyInfo(propertyNames[i], mayBeCollectionTypeName, null);
               }
               else
               {
                  throw e;
               }
            }
         }

         ret.add(new MappedClassInfo(mappedClass.getName(), identifierPropInfo, infos));
      }

      return ret;
   }
}
