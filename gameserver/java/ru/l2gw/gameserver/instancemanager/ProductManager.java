package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.ProductData;
import ru.l2gw.gameserver.serverpackets.ExBR_BuyProduct;
import ru.l2gw.gameserver.templates.StatsSet;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * @author: rage
 * @date: 15.10.11 19:43
 */
public class ProductManager
{
	private static final Log _log = LogFactory.getLog("product");
	private static final Log _logStd = LogFactory.getLog(ProductManager.class);
	private static final HashMap<Integer, GArray<ProductData>> productData = new HashMap<>();
	public static final GArray<ProductData> emptyData = new GArray<>(0);
	private static volatile int current_job;
	private static final FastMap<Integer, StatsSet> jobs = new FastMap<Integer, StatsSet>().shared();
	private static final FastMap<Integer, GArray<ProductData>> history = new FastMap<Integer, GArray<ProductData>>().shared();

	public static GArray<ProductData> getProductList()
	{
		if(!Config.PRODUCT_SHOP_ENABLED || !productData.containsKey(Config.PRODUCT_LOCATION_ID))
			return emptyData;

		GArray<ProductData> products = new GArray<>(productData.get(Config.PRODUCT_LOCATION_ID).size());
		for(ProductData pd : productData.get(Config.PRODUCT_LOCATION_ID))
			if(pd.buyable > 0 && pd.sale_start_date < System.currentTimeMillis() / 1000 && pd.sale_end_date > System.currentTimeMillis() / 1000)
				products.add(pd);

		return products;
	}

	public static ProductData getProductById(int productId)
	{
		for(ProductData pd : getProductList())
			if(pd.product_id == productId)
				return pd;

		return null;
	}

	public static synchronized StatsSet addJobForObjectId(int objectedId)
	{
		int jobId = getNextJobId();
		StatsSet job = new StatsSet();
		job.set("job_id", jobId);
		job.set("object_id", objectedId);
		jobs.put(jobId, job);
		return job;
	}

	public static synchronized StatsSet removeJob(int jobId)
	{
		return jobs.remove(jobId);
	}

	public static synchronized StatsSet getJob(int jobId)
	{
		return jobs.get(jobId);
	}

	public static synchronized boolean giveProduct(StatsSet job, long transaction)
	{
		L2Player player = L2ObjectsStorage.getPlayer(job.getInteger("object_id"));
		_log.info("give product: " + player + " [product_id=" + job.getInteger("product_id") + ";amount=" + job.getInteger("amount") + ";object_id=" + job.getInteger("object_id") + ";transaction=" + job.getInteger("transaction") + "]");
		if(player != null)
		{
			ProductData pd = getProductById(job.getInteger("product_id"));
			int amount = job.getInteger("amount");

			for(StatsSet ii : pd.items)
			{
				player.addItem("ProductBuy", ii.getInteger("item_id"), ii.getInteger("item_count") * amount, null, true);
			}

			Connection con = null;
			PreparedStatement stmt = null;

			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				stmt = con.prepareStatement("INSERT INTO product_buylog VALUES(?,?,?,?,?,?,?,?,now())");
				stmt.setInt(1, job.getInteger("job_id"));
				stmt.setInt(2, player.getNetConnection().getAccountId());
				stmt.setInt(3, player.getObjectId());
				stmt.setString(4, player.getName());
				stmt.setInt(5, job.getInteger("product_id"));
				stmt.setInt(6, job.getInteger("amount"));
				stmt.setInt(7, pd.price);
				stmt.setLong(8, transaction);
				stmt.execute();
			}
			catch(Exception e)
			{
				_log.warn("ProductManager: can't save product_buylog: " + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, stmt);
			}

			GArray<ProductData> buys = history.get(player.getObjectId());
			if(buys == null)
			{
				buys = new GArray<>();
				history.put(player.getObjectId(), buys);
			}

			if(!buys.contains(pd))
				buys.add(pd);

			player.sendPacket(new ExBR_BuyProduct(ExBR_BuyProduct.RESULT_OK));

			return true;
		}

		return false;
	}

	private static void loadProductData()
	{
		try
		{
			File file = new File(Config.PRODUCTDATA_FILE);

			if(!file.exists())
			{
				if(Config.PRODUCT_SHOP_ENABLED)
					_logStd.info("ProductManager: " + Config.PRODUCTDATA_FILE + " file is missing.");
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(file);
			DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
			int c = 0;
			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				try
				{
					if("products".equalsIgnoreCase(n.getNodeName()))
					{
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if("product".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attr = d.getAttributes();
								int loc_id = Integer.parseInt(attr.getNamedItem("location_id").getNodeValue());
								GArray<ProductData> products = productData.get(loc_id);
								if(products == null)
								{
									products = new GArray<>();
									productData.put(loc_id, products);
								}

								int id = Integer.parseInt(attr.getNamedItem("id").getNodeValue());
								String name = attr.getNamedItem("name").getNodeValue();
								int category = Integer.parseInt(attr.getNamedItem("category").getNodeValue());
								int price = Integer.parseInt(attr.getNamedItem("price").getNodeValue());
								int is_event_product = Integer.parseInt(attr.getNamedItem("is_event_product").getNodeValue());
								int is_best_product = Integer.parseInt(attr.getNamedItem("is_best_product").getNodeValue());
								int is_new_product = Integer.parseInt(attr.getNamedItem("is_new_product").getNodeValue());
								int buyable = Integer.parseInt(attr.getNamedItem("buyable").getNodeValue());
								Date startDate = df.parse(attr.getNamedItem("sale_start_date").getNodeValue());
								Date endDate = df.parse(attr.getNamedItem("sale_end_date").getNodeValue());
								String itemids = attr.getNamedItem("itemids").getNodeValue();

								products.add(new ProductData(id, name, category, price, is_event_product, is_best_product, is_new_product, buyable, (int) (startDate.getTime() / 1000), (int) (endDate.getTime() / 1000), loc_id, itemids));
								c++;
							}
						}
					}
				}
				catch(Exception e)
				{
					_logStd.warn("ProductManager: can't load product data" + e);
					e.printStackTrace();
				}
			}

			_logStd.info("ProductManager: loaded " + c + " products for " + productData.size() + " regions. Current region: " + Config.PRODUCT_LOCATION_ID);
		}
		catch(Exception e)
		{
			_logStd.warn("ProductManager: error while product data" + e);
			e.printStackTrace();
		}
	}

	public static void load()
	{
		_logStd.info("ProductManager: product shop " + (Config.PRODUCT_SHOP_ENABLED ? "enabled." : "disabled."));
		_logStd.info("ProductManager: loading product data.");

		loadProductData();

		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("SELECT max(job_id) job_id FROM product_buylog");
			rs = stmt.executeQuery();
			if(rs.next())
				current_job = rs.getInt("job_id");
		}
		catch(Exception e)
		{
			_logStd.warn("ProductManager: can't load last job_id: " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt, rs);
		}
	}

	public static void reloadProductData()
	{
		productData.clear();
		loadProductData();
	}

	public static GArray<ProductData> getBuyHistory(int objectId)
	{
		GArray<ProductData> buys = history.get(objectId);
		if(buys == null)
		{
			buys = new GArray<>();
			history.put(objectId, buys);

			Connection con = null;
			PreparedStatement stmt = null;
			ResultSet rs = null;

			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				stmt = con.prepareStatement("SELECT distinct(product_id) product_id FROM product_buylog ORDER BY buy_date DESC LIMIT 20");
				rs = stmt.executeQuery();
				while(rs.next())
				{
					ProductData pd = getProductById(rs.getInt("product_id"));
					if(pd != null)
						buys.add(pd);
				}
			}
			catch(Exception e)
			{
				_log.warn("ProductManager: can't load last job_id: " + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, stmt, rs);
			}
		}

		return buys;
	}

	public static synchronized int getNextJobId()
	{
		return ++current_job;
	}
}
