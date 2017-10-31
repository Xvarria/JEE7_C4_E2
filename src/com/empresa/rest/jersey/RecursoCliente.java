package com.empresa.rest.jersey;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.empresa.rest.model.Cliente;

@Path("/clientes")
public class RecursoCliente {
	private Map<Integer, Cliente> customerDB = new ConcurrentHashMap<Integer, Cliente>();
	private AtomicInteger idCounter = new AtomicInteger();

	public RecursoCliente() {
		this.addCliente();
	}

	@POST
	@Consumes("application/xml")
	public Response addCliente(InputStream is) {
		Cliente cliente = readCliente(is);
		cliente.setId(idCounter.incrementAndGet());
		customerDB.put(cliente.getId(), cliente);
		System.out.println("Created	customer	" + cliente.getId());
		return Response.created(URI.create("/clientes/" + cliente.getId())).build();
	}

	private void addCliente() {
		Cliente cliente = new Cliente();
		cliente.setId(idCounter.incrementAndGet());
		cliente.setNombre("Will");
		cliente.setApellido("Smith");
		cliente.setTelefono("1234567");
		cliente.setEmail("correo@correo.com");
		customerDB.put(cliente.getId(), cliente);
	}

	@GET
	@Path("{id}")
	@Produces("application/xml")
	public StreamingOutput getCliente(@PathParam("id") int id) {
		final Cliente cliente = customerDB.get(id);
		if (cliente == null) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		return new StreamingOutput() {
			public void write(OutputStream outputStream) throws IOException, 
			                                                    WebApplicationException {
				outputCliente(outputStream, cliente);
			}
		};
	}

	@PUT
	@Path("{id}")
	@Consumes("application/xml")
	public void updateCliente(@PathParam("id") int id, InputStream is) {
		Cliente update = readCliente(is);
		Cliente current = customerDB.get(id);
		if (current == null)
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		current.setNombre  (update.getNombre()  );
		current.setApellido(update.getApellido());
		current.setTelefono(update.getTelefono());
		current.setEmail   (update.getEmail()   );
	}

	protected void outputCliente(OutputStream os, Cliente cliente) throws IOException {
		PrintStream writer = new PrintStream(os);
		writer.println("<cliente	id=\"" + cliente.getId() + "\">");
		writer.println("			<nombre>" + cliente.getNombre() + "</nombre>");
		writer.println("			<apellido>" + cliente.getApellido() + "</apellido>");
		writer.println("			<telefono>" + cliente.getTelefono() + "</telefono>");
		writer.println("			<email>" + cliente.getEmail() + "</email>");
		writer.println("</cliente>");
	}

	protected Cliente readCliente(InputStream is) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(is);
			Element root = doc.getDocumentElement();
			Cliente cliente = new Cliente();
			if (root.getAttribute("id") != null && !root.getAttribute("id").trim().equals("")) {
				cliente.setId(Integer.valueOf(root.getAttribute("id")));
			}
			NodeList nodes = root.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				Element element = (Element) nodes.item(i);
				if (element.getTagName().equals("nombre")) {
					cliente.setNombre(element.getTextContent());
				} else if (element.getTagName().equals("apellido")) {
					cliente.setApellido(element.getTextContent());
				} else if (element.getTagName().equals("telefono")) {
					cliente.setTelefono(element.getTextContent());
				} else if (element.getTagName().equals("email")) {
					cliente.setEmail(element.getTextContent());
				}
			}
			return cliente;
		} catch (Exception e) {
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}
	}
}
