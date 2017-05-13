package ru.l2gw.extensions.scripts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.scripts.Compiler.MemoryClassLoader;
import ru.l2gw.extensions.scripts.jarloader.JarClassLoader;
import ru.l2gw.gameserver.GameServer;
import ru.l2gw.gameserver.handler.*;
import ru.l2gw.gameserver.instancemanager.QuestManager;
import ru.l2gw.gameserver.model.quest.Quest;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Scripts
{
	private static final Log _log = LogFactory.getLog(Scripts.class.getName());

	public static boolean JAR;

	private static Scripts _instance;
	private HashMap<String, Script> _classes = new HashMap<String, Script>();
	public static HashMap<Integer, ArrayList<ScriptClassAndMethod>> itemHandlers = new HashMap<Integer, ArrayList<ScriptClassAndMethod>>();
	public static HashMap<Integer, ArrayList<ScriptClassAndMethod>> dialogAppends = new HashMap<Integer, ArrayList<ScriptClassAndMethod>>();
	public static HashMap<String, ScriptClassAndMethod> onAction = new HashMap<String, ScriptClassAndMethod>();
	public static HashMap<String, ScriptClassAndMethod> onActionShift = new HashMap<String, ScriptClassAndMethod>();
	public static ArrayList<ScriptClassAndMethod> onPlayerExit = new ArrayList<ScriptClassAndMethod>();
	public static ArrayList<ScriptClassAndMethod> onPlayerEnter = new ArrayList<ScriptClassAndMethod>();
	public static ArrayList<ScriptClassAndMethod> onPlayerSkillsRestored = new ArrayList<ScriptClassAndMethod>();
	public static ArrayList<ScriptClassAndMethod> onPlayerSkillAdd = new ArrayList<ScriptClassAndMethod>();
	public static ArrayList<ScriptClassAndMethod> onPlayerClassChange = new ArrayList<ScriptClassAndMethod>();
	public static ArrayList<ScriptClassAndMethod> onReloadMultiSell = new ArrayList<ScriptClassAndMethod>();
	public static ArrayList<ScriptClassAndMethod> onItemAdded = new ArrayList<ScriptClassAndMethod>();
	public static ArrayList<ScriptClassAndMethod> onItemRemoved = new ArrayList<ScriptClassAndMethod>();
	public static ArrayList<ScriptClassAndMethod> onLevelUp = new ArrayList<ScriptClassAndMethod>();

	public static boolean loading;

	public static Scripts getInstance()
	{
		if(_instance == null)
			new Scripts();
		return _instance;
	}

	public Scripts()
	{
		_instance = this;
		load(false);
	}

	public boolean reload()
	{
		loading = true;

		for(ScriptObject go : GameServer.scriptsObjects.values())
			try
			{
				go.invokeMethod("onReload");
			}
			catch(Exception f)
			{
				f.printStackTrace();
			}
		GameServer.scriptsObjects.clear();

		boolean error = load(true);
		callOnLoad();

		loading = false;
		return error;
	}

	public void shutdown()
	{
		for(ScriptObject go : GameServer.scriptsObjects.values())
			try
			{
				go.invokeMethod("onShutdown");
			}
			catch(Exception f)
			{
				f.printStackTrace();
			}
		GameServer.scriptsObjects.clear();
	}

	private boolean load(boolean reload)
	{
		_log.info("Scripts loading...");
		boolean error = false;
		Class<?> c;

		JAR = new File("./scripts.jar").exists();

		if(JAR)
		{
			JarClassLoader jcl;
			try
			{
				jcl = new JarClassLoader("./scripts.jar");
				for(String name : jcl.getClassNames())
				{
					if(!name.contains(".class"))
						continue;
					if(name.contains("$"))
						continue; // пропускаем вложенные классы
					name = name.replace(".class", "").replace("/", ".");
					c = jcl.loadClass(name);
					Script s = new Script(c);
					_classes.put(c.getName(), s);
				}
			}
			catch(Exception e)
			{
				error = true;
				e.printStackTrace();
			}
		}

		ArrayList<File> scriptFiles = new ArrayList<File>();
		parseClasses(new File("./data/scripts"), scriptFiles);
		if(!scriptFiles.isEmpty())
		{

			if(Compiler.getInstance().compile(scriptFiles, System.out))
			{
				MemoryClassLoader classLoader = Compiler.getInstance().classLoader; //TODO				
				for(String name : classLoader.byteCodes.keySet())
				{
					if(name.contains("$"))
						continue; // пропускаем вложенные классы
					try
					{
						c = classLoader.loadClass(name);
						Script s = new Script(c);
						_classes.put(name, s);
					}
					catch(ClassNotFoundException e)
					{
						_log.warn("Can't load script class:" + e.getMessage());
						error = true;
					}
				}
				Compiler.getInstance().classLoader = null;
			}
			else
			{
				_log.warn("Can't compile scripts!");
				error = true;
			}
		}

		if(error)
		{
			_log.info("Scripts loaded with errors. Loaded " + _classes.size() + " classes.");
			if(!reload)
				Runtime.getRuntime().halt(0);
		}
		else
			_log.info("Scripts successfully loaded. Loaded " + _classes.size() + " classes.");
		return error;
	}

	public void callOnLoad()
	{
		loadAndInitHandlers();
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminScripts());
	}

	private void loadAndInitHandlers()
	{
		itemHandlers.clear();
		dialogAppends.clear();
		onAction.clear();
		onActionShift.clear();
		onPlayerExit.clear();
		onPlayerEnter.clear();
        onPlayerSkillsRestored.clear();
        onPlayerSkillAdd.clear();
        onPlayerClassChange.clear();
		onReloadMultiSell.clear();
		onItemAdded.clear();
		onItemRemoved.clear();
		onLevelUp.clear();
		List<Script> sorted = new ArrayList<Script>(_classes.values());
		Collections.sort(sorted);

		for(Script _class : sorted)
			try
			{
				if(!GameServer.scriptsObjects.containsKey(_class.getName()))
				{
					ScriptObject go = _class.newInstance();
					GameServer.scriptsObjects.put(_class.getName(), go);
					go.invokeMethod("onLoad");
					if(go.getScriptInstance() instanceof IOnDieHandler)
						ScriptHandler.getInstance().registerOnDieHandler((IOnDieHandler) go.getScriptInstance());
					if(go.getScriptInstance() instanceof IOnResurrectHandler)
						ScriptHandler.getInstance().registerOnResurrectHandler((IOnResurrectHandler) go.getScriptInstance());
					if(go.getScriptInstance() instanceof IOnEscapeHandler)
						ScriptHandler.getInstance().registerOnEscapeHandler((IOnEscapeHandler) go.getScriptInstance());
				}

				for(Method method : _class.getRawClass().getMethods())
					if(method.getName().contains("ItemHandler_"))
					{
						Integer id = Integer.parseInt(method.getName().substring(12));
						ArrayList<ScriptClassAndMethod> handlers = itemHandlers.get(id);
						if(handlers == null)
						{
							handlers = new ArrayList<ScriptClassAndMethod>();
							itemHandlers.put(id, handlers);
						}
						handlers.add(new ScriptClassAndMethod(_class, method));
					}
					else if(method.getName().contains("DialogAppend_"))
					{
						Integer id = Integer.parseInt(method.getName().substring(13));
						ArrayList<ScriptClassAndMethod> handlers = dialogAppends.get(id);
						if(handlers == null)
						{
							handlers = new ArrayList<ScriptClassAndMethod>();
							dialogAppends.put(id, handlers);
						}
						handlers.add(new ScriptClassAndMethod(_class, method));
					}
					else if(method.getName().contains("OnAction_"))
					{
						String name = method.getName().substring(9);
						if(onAction.containsKey(name))
							onAction.remove(name);
						onAction.put(name, new ScriptClassAndMethod(_class, method));
					}
					else if(method.getName().contains("OnActionShift_"))
					{
						String name = method.getName().substring(14);
						if(onActionShift.containsKey(name))
							onActionShift.remove(name);
						onActionShift.put(name, new ScriptClassAndMethod(_class, method));
					}
					else if(method.getName().equals("OnPlayerExit"))
						onPlayerExit.add(new ScriptClassAndMethod(_class, method));
					else if(method.getName().equals("OnPlayerEnter"))
						onPlayerEnter.add(new ScriptClassAndMethod(_class, method));
					else if(method.getName().equals("onPlayerSkillsRestored"))
						onPlayerSkillsRestored.add(new ScriptClassAndMethod(_class, method));
					else if(method.getName().equals("onPlayerSkillAdd"))
						onPlayerSkillAdd.add(new ScriptClassAndMethod(_class, method));
					else if(method.getName().equals("onPlayerClassChange"))
						onPlayerClassChange.add(new ScriptClassAndMethod(_class, method));
					else if(method.getName().equals("OnReloadMultiSell"))
						onReloadMultiSell.add(new ScriptClassAndMethod(_class, method));
					else if(method.getName().equals("onItemAdded"))
						onItemAdded.add(new ScriptClassAndMethod(_class, method));
					else if(method.getName().equals("onItemRemoved"))
						onItemRemoved.add(new ScriptClassAndMethod(_class, method));
					else if(method.getName().equals("onLevelUp"))
						onLevelUp.add(new ScriptClassAndMethod(_class, method));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
	}

	public boolean reloadClass(String name)
	{
		File f = new File("./data/scripts/" + name.replace(".", "/") + ".java");
		if(f.exists() && f.isFile())
			return reloadClassByName(name.replace("/", "."));

		f = new File("./data/scripts/" + name.replace(".", "/"));
		if(f.exists() && f.isDirectory())
			return reloadClassByPath(f);

		_log.warn("Con't find class or package by path: " + name);
		return true;
	}

	public boolean reloadClassByPath(File f)
	{
		ArrayList<File> scriptFiles = new ArrayList<File>();
		parseClasses(f, scriptFiles);
		if(Compiler.getInstance().compile(scriptFiles, System.out))
		{
			MemoryClassLoader classLoader = Compiler.getInstance().classLoader;
			Class<?> c;
			for(String name : classLoader.byteCodes.keySet())
			{
				if(name.contains("$"))
					continue; // пропускаем вложенные классы
				try
				{
					c = classLoader.loadClass(name);
					Script s = new Script(c);
					_classes.put(name, s);
					ScriptObject so = GameServer.scriptsObjects.remove(name);
					if(so != null)
						so.invokeMethod("onReload");
				}
				catch(ClassNotFoundException e)
				{
					_log.warn("Can't load script class:" + e.getMessage());
					return true;
				}
			}
			Compiler.getInstance().classLoader = null;
			loadAndInitHandlers();
		}
		else
		{
			_log.warn("Can't recompile scripts: " + f.getPath());
			return true;
		}
		return false;
	}

	public boolean reloadClassByName(String name)
	{
		if(Compiler.getInstance().compile(new File("./data/scripts/" + name.replace(".", "/") + ".java"), System.out))
		{
			MemoryClassLoader classLoader = Compiler.getInstance().classLoader;
			try
			{
				Class<?> c = classLoader.loadClass(name);
				Script s = new Script(c);
				ScriptObject so = GameServer.scriptsObjects.remove(name);
				if(so != null)
					so.invokeMethod("onReload");
				_classes.put(name, s);
				loadAndInitHandlers();
				return false;
			}
			catch(ClassNotFoundException e)
			{
				_log.warn("Can't load script class:" + e.getMessage());
			}
			Compiler.getInstance().classLoader = null;
		}
		else
		{
			_log.warn("Can't recompile script: " + name);
		}
		return true;
	}

	public boolean reloadQuest(String name)
	{
		if(Config.DONTLOADQUEST)
			return true;
		Quest q = QuestManager.getQuest(name);
		File f;
		if(q != null)
		{
			String path = q.getClass().getPackage().getName().replace(".", "/");
			f = new File("./data/scripts/" + path + "/");
			if(f.isDirectory())
				return reloadClassByPath(f);
		}
		q = QuestManager.getQuest(Integer.parseInt(name));
		if(q != null)
		{
			String path = q.getClass().getPackage().getName().replace(".", "/");
			f = new File("./data/scripts/" + path + "/");
			if(f.isDirectory())
				return reloadClassByPath(f);
		}
		return reloadClassByPath(new File("./data/scripts/quests/" + name + "/"));
	}

	private void parseClasses(File f, ArrayList<File> list)
	{
		for(File z : f.listFiles())
			if(z.isDirectory())
			{
				if(z.isHidden() || z.getName().equals(".svn"))
					continue;
				if(Config.DONTLOADQUEST && z.getName().equals("quests") && z.getParentFile().getName().equals("scripts"))
					continue;
				parseClasses(z, list);
			}
			else
			{
				if(z.isHidden() || !z.getName().contains(".java"))
					continue;
				list.add(z);
			}
	}

	public HashMap<String, Script> getClasses()
	{
		return _classes;
	}

	public static class ScriptClassAndMethod
	{
		public final Script scriptClass;
		public final Method method;

		public ScriptClassAndMethod(Script _scriptClass, Method _method)
		{
			scriptClass = _scriptClass;
			method = _method;
		}
	}
}
