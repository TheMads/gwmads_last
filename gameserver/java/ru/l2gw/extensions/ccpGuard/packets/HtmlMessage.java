package ru.l2gw.extensions.ccpGuard.packets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.util.Files;

import java.util.ArrayList;

public class HtmlMessage extends L2GameServerPacket
{
	private static final Log _log = LogFactory.getLog(HtmlMessage.class.getName());
	private int _npcObjId;
	private String _html;
	private String _file = null;
	private ArrayList<String> replaces = new ArrayList<String>();
	private int item_id = 0;
	private boolean can_writeImpl = false;
	private boolean have_appends = false;


	public HtmlMessage(int npcObjId)
	{
		_npcObjId = npcObjId;
	}

	public final HtmlMessage setHtml(String text)
	{
		if(!text.contains("<html>"))
			text = "<html><body>" + text + "</body></html>"; //<title>Message:</title> <br><br><br>
		_html = text;
		return this;
	}

	public final HtmlMessage setFile(String file)
	{
		_file = file;
		return this;
	}

	public final HtmlMessage setItemId(int _item_id)
	{
		item_id = _item_id;
		return this;
	}

	private void setFile()
	{
		String content = loadHtml(_file, getClient().getPlayer());
		if(content == null)
			setHtml(have_appends && _file.endsWith(".htm") ? "" : _file);
		else
			setHtml(content);
	}

	protected String loadHtml(String name, L2Player player)
	{
		return Files.read(name, player);
	}

	protected String html_load(String name, String lang)
	{
		String content = Files.read(name, lang);
		if(content == null)
			content = "Can't find file'" + name + "'";
		return content;
	}

	public void replace(String pattern, String value)
	{
		replaces.add(pattern);
		replaces.add(value);
	}

	@Override
	final public void runImpl()
	{
		if(_file != null)
			setFile();

		for(int i = 0; i < replaces.size(); i += 2)
			_html = _html.replaceAll(replaces.get(i), replaces.get(i + 1));
		_html = _html.replaceAll("%objectId%", String.valueOf(_npcObjId));

		if(_html.length() > 8192)
		{
			_html = "<html><body><center>Sorry, to long html.</center></body></html>";
		}

		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;
		writeC(0x19);
		writeD(_npcObjId);
		writeS(_html);
		writeD(item_id);
	}
}