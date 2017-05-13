package ru.l2gw.gameserver.tables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.model.instances.L2HennaInstance;
import ru.l2gw.gameserver.templates.L2Henna;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class HennaTreeTable
{
	private static Log _log = LogFactory.getLog(HennaTreeTable.class.getName());
	private static final HennaTreeTable _instance = new HennaTreeTable();
	private HashMap<ClassId, ArrayList<L2HennaInstance>> _hennaTrees;
	private boolean _initialized = true;

	public static HennaTreeTable getInstance()
	{
		return _instance;
	}

	private HennaTreeTable()
	{
		_hennaTrees = new HashMap<>();
		int classId = 0;
		int count = 0;
		Connection con = null;
		PreparedStatement statement = null;
		PreparedStatement statement2 = null;
		ResultSet classlist = null, hennatree = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT class_name, id, parent_id, parent_id2 FROM class_list ORDER BY id");
			statement2 = con.prepareStatement("SELECT class_id, symbol_id FROM henna_trees where class_id=? ORDER BY symbol_id");
			classlist = statement.executeQuery();
			ArrayList<L2HennaInstance> list = new ArrayList<L2HennaInstance>();
			//int parentClassId;
			//L2Henna henna;
			while(classlist.next())
			{
				list = new ArrayList<L2HennaInstance>();
				classId = classlist.getInt("id");
				statement2.setInt(1, classId);
				hennatree = statement2.executeQuery();
				while(hennatree.next())
				{
					short id = hennatree.getShort("symbol_id");
					//String name = hennatree.getString("name");
					L2Henna template = HennaTable.getInstance().getTemplate(id);
					if(template == null)
						return;
					L2HennaInstance temp = new L2HennaInstance(template);
					temp.setSymbolId(id);
					temp.setItemIdDye(template.getDyeId());
					temp.setAmountDyeRequire(template.getAmountDyeRequire());
					temp.setPrice(template.getPrice());
					temp.setStatINT(template.getStatINT());
					temp.setStatSTR(template.getStatSTR());
					temp.setStatCON(template.getStatCON());
					temp.setStatMEM(template.getStatMEM());
					temp.setStatDEX(template.getStatDEX());
					temp.setStatWIT(template.getStatWIT());

					list.add(temp);
				}
				_hennaTrees.put(ClassId.values()[classId], list);
				count += list.size();
				_log.debug("Henna Tree for Class: " + classId + " has " + list.size() + " Henna Templates.");
				DbUtils.closeQuietly(hennatree);
			}
		}
		catch(Exception e)
		{
			_log.warn("error while creating henna tree for classId " + classId + "	" + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(statement2, hennatree);
			DbUtils.closeQuietly(con, statement, classlist);
		}

		_log.info("HennaTreeTable: Loaded " + count + " Henna Tree Templates.");

	}

	public L2HennaInstance[] getAvailableHenna(ClassId classId, byte sex)
	{
		if(classId.getId() > 135)
			classId = classId.getParent(sex);

		ArrayList<L2HennaInstance> henna = _hennaTrees.get(classId);
		if(henna == null || henna.size() == 0)
		{
			// the hennatree for this class is undefined, so we give an empty list
			if(Config.DEBUG)
				_log.warn("Hennatree for class " + classId + " is not defined !");
			return new L2HennaInstance[0];
		}
		return henna.toArray(new L2HennaInstance[henna.size()]);
	}

	public boolean isInitialized()
	{
		return _initialized;
	}
}
