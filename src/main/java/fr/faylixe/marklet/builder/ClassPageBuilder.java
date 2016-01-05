package fr.faylixe.marklet.builder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.PackageDoc;

import fr.faylixe.marklet.IGenerationContext;
import fr.faylixe.marklet.MarkdownUtils;
import fr.faylixe.marklet.MarkletConstant;

/**
 * 
 * @author fv
 */
public final class ClassPageBuilder {

	/** */
	private final IGenerationContext context;

	/** Document builder instance for filling class page content.  **/
	private final DocumentBuilder documentBuilder;

	/** Target class that page is built from. **/
	private final ClassDoc classDoc;

	/**
	 * Default constructor. 
	 * 
	 * @param context
	 * @param documentBuilder Document builder instance for filling class page content.
	 * @param classDoc Target class that page is built from.
	 */
	private ClassPageBuilder(final IGenerationContext context, final DocumentBuilder documentBuilder, final ClassDoc classDoc) {
		this.context = context;
		this.classDoc = classDoc;
		this.documentBuilder = documentBuilder;
	}
	
	/**
	 * Indicates if the target class exposes any method.
	 * 
	 * @return ``true`` if the target class exposes at least one method, ``false`` otherwise.
	 */
	private boolean hasMethod() {
		return classDoc.methods().length > 0;
	}
	
	/**
	 * Indicates if the target class exposes any field.
	 * 
	 * @return ``true`` if the target class exposes at least one field, ``false`` otherwise.
	 */
	private boolean hasField() {
		return classDoc.fields().length > 0;
	}

	/**
	 * Builds and writes the documentation header.
	 * Consists in the class name with a H1 level,
	 * the class hierarchy, and the comment text.
	 * 
	 * TODO : Processes link tags.
	 * 
	 * @throws IOException If any error occurs while writing header.
	 */
	private void buildHeader() throws IOException {
		documentBuilder.appendHeader(classDoc.name(), 1);
		final PackageDoc packageDoc = classDoc.containingPackage();
		final String packageName = packageDoc.name();
		documentBuilder.appendText(MarkletConstant.PACKAGE + MarkdownUtils.buildLink(packageName, "README.md") + "<br>");
		documentBuilder.appendHierarchy(classDoc);
		documentBuilder.appendText(classDoc.commentText());		
	}

	/**
	 * Returns an ordered stream of element that are provided
	 * by the given ``supplier``, using element name for sorting.
	 * 
	 * @param supplier Supplier that provides elements to stream.
	 * @return Ordered stream.
	 */
	private <T extends Doc> Stream<T> getOrderedElements(final Supplier<T[]> supplier) {
		return Arrays
				.stream(supplier.get())
				.sorted((a, b) -> {
					return a.name().compareTo(b.name());
				});
	}

	/**
	 * Builds class summary. Consists in an overview of
	 * available constructor, method, and field, in a
	 * table form.
	 * 
	 * @throws IOException If any error occurs while writing summary.
	 */
	private void buildSummary() throws IOException {
		if (hasField() || hasMethod()) {
			documentBuilder.newLine();
			documentBuilder.appendHeader("Summary", 2);
			if (hasMethod()) {
				documentBuilder.initializeMethodHeader();
				getOrderedElements(classDoc::methods).forEach(method -> {
					try {
						documentBuilder.appendMethodHeader(method);				
					}
					catch (final IOException e) {
						// TODO : Throw runtime here.
					}
				});
			}
		}
	}
	
	/**
	 * Builds fields documentation.
	 * 
	 * @throws IOException If any error occurs while writing field documentation.
	 */
	private void buildFields() throws IOException {
		if (hasField()) {
			documentBuilder.newLine();
			documentBuilder.appendHeader(MarkletConstant.FIELDS, 2);
			documentBuilder.initializeFieldHeader();
			buildFields(getOrderedElements(classDoc::fields).filter(field -> !field.isStatic()));
			buildFields(getOrderedElements(classDoc::fields).filter(FieldDoc::isStatic));
		}
	}
	
	/**
	 * 
	 * @param stream
	 */
	private void buildFields(final Stream<FieldDoc> stream) {
		stream.forEach(field -> {
			try {
				documentBuilder.appendField(field);
			}
			catch (final IOException e) {
				// TODO : Throw runtine here.
			}
		});
	}
	
	/**
	 * Builds methods documentation.
	 * 
	 * @throws IOException If any error occurs while writing method documentation.
	 */
	private void buildMethods() throws IOException {
		if (hasMethod()) {
			documentBuilder.newLine();
			documentBuilder.appendHeader(MarkletConstant.METHODS, 2);
			getOrderedElements(classDoc::methods).forEach(method -> {
				try {
					documentBuilder.appendMethod(method);
				}
				catch (final IOException e) {
					// TODO : Throw runtine here.
				}
			});
		}
	}

	/**
	 * Builds and writes the documentation file
	 * associated to the given <tt>classDoc</tt>
	 * into the directory denoted by the given path.
	 * 
	 * @param context Context used.
	 * @param classDoc Class to generated documentation for.
	 * @param directoryPath Path of the directory to write documentation in.
	 * @throws IOException If any error occurs while writing documentation.
	 */
	public static void build(final IGenerationContext context, final ClassDoc classDoc, final Path directoryPath) throws IOException {
		final Path classPath = Paths.get(
				new StringBuilder()
					.append(classDoc.simpleTypeName())
					.append(IGenerationContext.FILE_EXTENSION)
					.toString());
		final DocumentBuilder documentBuilder = DocumentBuilder.create(context, classDoc.containingPackage(), directoryPath.resolve(classPath));
		final ClassPageBuilder builder = new ClassPageBuilder(context, documentBuilder, classDoc);
		builder.buildHeader();
		builder.buildSummary();
		builder.buildFields();
		builder.buildMethods();
		documentBuilder.build();
	}

}