/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.forum.rendering.MarkupRenderingService;
import org.exoplatform.forum.rendering.api.Renderer;
import org.exoplatform.forum.rendering.core.SupportedSyntaxes;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.quartz.JobExecutionContext;
import org.w3c.dom.Document;


/**
 * @version $Revision$
 */
public class CommonUtils {
  private static Log log = ExoLogger.getLogger(CommonUtils.class);
  
  public static final String         COMMA        = ",";

  public static final String         SLASH        = "/";

  public static final String         EMPTY_STR    = "";

  public static final String         COLON        = ":";

  public static final String         SEMICOLON    = ";";

  public static final String         SPACE        = " ";
  
  public static final String         ASTERISK_STR = "*";

  public static final String         PERCENT_STR  = "%";

  public static final String         AMP_NUMBER   = "&#";

  public static final String         LESS_THAN    = "&lt;";

  public static final String         GREATER_THAN = "&gt;";

  public static final String         QUOT         = "&quot;";

  public static final String         AMP_SPACE    = "&nbsp;";

  public static final String         AMP_HEX      = "&#x26;";

  public static final String         AMP          = "&amp;";

  public static final String         FROM_KEY     = "gatein.email.smtp.from";

  private static final String        SPECIAL_CHARACTOR_FORSERACH_REGEX = "[^\\pL\\pM\\p{Nd}\\p{Nl}\\p{Pc}[\\p{InEnclosedAlphanumerics}&&\\p{So}]\\?\\*%0-9]";
  
  private static final String        SPECIAL_CHARACTOR_FOR_UNIFIED_SERACH_REGEX = "[^\\pL\\pM\\p{Nd}\\p{Nl}\\p{Pc}[\\p{InEnclosedAlphanumerics}&&\\p{So}]0-9]";
  
  private static final String        SPECIAL_CHARACTOR_REGEX = "[^\\pL\\pM\\p{Nd}\\p{Nl}\\p{Pc}[\\p{InEnclosedAlphanumerics}&&\\p{So}]\\ %0-9]";

  private static List<String>        tokens     = new ArrayList<String>();

  private static Map<String, String> charcodes  = new HashMap<String, String>();

  private static List<String> ignoreLessThanAndGreaterThan = Arrays.asList(LESS_THAN, GREATER_THAN, AMP);
  
  private static final Pattern EXCEPT_PATTERN = Pattern.compile("~(([1|0]\\.[0-9])|1)+");
  /*
   *  The distance code number content special character.
   *  Ex: from ' '(32) to '0'(48): ' ', '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/'
   *  See: http://www.ascii.cl/htmlcodes.htm
  */
  private static int[] CHAR_CODES = new int[] { 48, 32, 65, 57, 97, 90, 127, 122, 39 };// '0', ' ', 'A', '9', 'a', 'Z', '~', 'z', '\''
  
  
  /**
   * Generates checksum for files, and get a file ending in .sha1 
   * This task can also be used to perform checksum verifications. 
   * 
   * @param b
   * @return
   * @throws Exception
   */
  static public String generateCheckSum(byte[] b) throws Exception {
    try{
      MessageDigest md = MessageDigest.getInstance("SHA1");
      md.update(b) ;
      byte[] mdbytes = md.digest();
   
      //convert the byte to hex format
      StringBuffer sb = new StringBuffer(EMPTY_STR);
      for (int i = 0; i < mdbytes.length; i++) {
        sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
      }
       return sb.toString() ;
    }catch(Exception e) {
      log.warn("Can not generate checksum for exporting data") ;
      return EMPTY_STR ;
    }
  }
  
  static public File getXMLFile(ByteArrayOutputStream bos, String appName, String objectType, Date createDate, String fileName) throws Exception {
    byte[] byteData = bos.toByteArray() ;
    
    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();   
    InputStream is  = new ByteArrayInputStream(byteData) ;
    Document document = docBuilder.parse(is) ;
    
    org.w3c.dom.Attr namespace = document.createAttribute("xmlns:exoks") ;
    namespace.setValue("http://www.exoplatform.com/exoks/2.0") ;
    document.getFirstChild().getAttributes().setNamedItem(namespace) ;
    
    org.w3c.dom.Attr attName = document.createAttribute("exoks:applicationName") ;
    attName.setValue(appName) ;
    document.getFirstChild().getAttributes().setNamedItem(attName) ;
    
    org.w3c.dom.Attr dataType = document.createAttribute("exoks:objectType") ;
    dataType.setValue(objectType) ;
    document.getFirstChild().getAttributes().setNamedItem(dataType) ;
    
    org.w3c.dom.Attr exportDate = document.createAttribute("exoks:exportDate") ;
    exportDate.setValue(createDate.toString()) ;
    document.getFirstChild().getAttributes().setNamedItem(exportDate) ;
    
    org.w3c.dom.Attr checkSum = document.createAttribute("exoks:checkSum") ;
    checkSum.setValue(generateCheckSum(byteData)) ;
    document.getFirstChild().getAttributes().setNamedItem(checkSum) ;
    
    DOMSource source = new DOMSource(document.getFirstChild()) ;
    
    File file = new File(fileName + ".xml");
    file.deleteOnExit();
    file.createNewFile();
    StreamResult result = new StreamResult(file) ;
    TransformerFactory tFactory = TransformerFactory.newInstance();
    Transformer transformer = tFactory.newTransformer();
    transformer.transform(source, result) ;
    return file ;
  }
  
  public static String getImageUrl(String imagePath) throws Exception {
    StringBuilder url = new StringBuilder() ;
    try {
      ExoContainerContext exoContext = getComponent(ExoContainerContext.class);
      url.append(SLASH).append(exoContext.getRestContextName());
    } catch (Exception e) {
      url.append("/portal");
      log.error("Can not get portal name or rest context name, exception: ",e);
    }
    RepositoryService rService = getComponent(RepositoryService.class) ;
    url.append("/jcr/").append(rService.getCurrentRepository().getConfiguration().getName()).append(imagePath).append(SLASH);
    return url.toString();
  }
  
  public static String convertCodeHTML(String s) {
    if (isEmpty(s))
      return EMPTY_STR;
    s = s.replaceAll("(<p>((\\&nbsp;)*)(\\s*)?</p>)|(<p>((\\&nbsp;)*)?(\\s*)</p>)", "<br/>").trim();
    s = s.replaceFirst("(<br/>)*", EMPTY_STR);
    s = s.replaceAll("(\\w|\\$)(>?,?\\.?\\*?\\!?\\&?\\%?\\]?\\)?\\}?)(<br/><br/>)*", "$1$2");
    try {
      s = processBBCode(s);
      s = s.replaceAll("(https?|ftp)://", " $0").replaceAll("(=\"|=\'|\'>|\">)( )(https?|ftp)", "$1$3")
           .replaceAll("[^=\"|^=\'|^\'>|^\">](https?://|ftp://)([-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])", "<a target=\"_blank\" href=\"$1$2\">$1$2</a>");
      s = s.replaceAll("&apos;", "'");
    } catch (Exception e) {
      log.error("Failed to convert HTML" + e.getMessage());
    }
    return s;
  }
  
  /**
   * This function will change email address in 'from' field by address of mail service 
   * which is configured as system property : <code>gatein.email.smtp.from</code> or <code>mail.from</code>. <br>
   * That ensures that 'emailAddress' part of 'from' field in a message object is always the same identity
   * with authentication of smtp configuration.<br> It's because of 2 reasons: 
   * <li>we don't want notification message to show email address of user as sender. Instead, we use mail service of kernel.</li>
   * <li>Almost authenticated smtp systems do not allow to separate email address in <code>from</code> field 
   * of message from smtp authentication</b> (for now, GMX, MS exchange deny, Gmail efforts to modify the such value)</li>
   * 
   * @param from
   * @return null if can not find suitable sender.
   */
  public static String makeNotificationSender(String from) {
    if (from == null)
      return null;
    Properties props = new Properties(System.getProperties());
    String mailAddr = props.getProperty(FROM_KEY);
    if (mailAddr == null || mailAddr.length() == 0)
      mailAddr = props.getProperty("mail.from");
    if (mailAddr != null) {
      try {
        return new InternetAddress(from + "<" + mailAddr + ">").toUnicodeString();
      } catch (AddressException e) {
        if (log.isDebugEnabled()) {
          log.debug("value of 'gatein.email.smtp.from' or 'mail.from' in configuration file is not in format of mail address", e);
        }
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * Get the URI of full URL
   * 
   * @param url The URL input
   * @return
   */
  public static String getURI(String url) {
    if (!isEmpty(url) && url.indexOf("http") == 0 && url.indexOf(SLASH, 8) > 0) {
      return url.substring(url.indexOf(SLASH, 8));
    }
    return url;
  }

  /**
   * @param s
   * @return
   */
  public static String processBBCode(String s) {
    MarkupRenderingService markupRenderingService = getComponent(MarkupRenderingService.class);
    Renderer r = markupRenderingService.getRenderer(SupportedSyntaxes.bbcode.name());
    return r.render(s);
  }
  
  /**
   * Get a Component from the current container context
   * @param <T> type of the expected component
   * @param type key for the component
   * @return
   */
  
  public static ExoContainer getExoContainer(JobExecutionContext context) {
    if(context == null) return null;
    String portalName = context.getJobDetail().getKey().getGroup();
    if(portalName == null) {
      portalName = PortalContainer.getCurrentPortalContainerName();
    }
    if(portalName.indexOf(COLON) > 0) portalName = portalName.substring(0, portalName.indexOf(":"));
    return ExoContainerContext.getContainerByName(portalName);
  }
  
  public static String getRSSLink(String appType, String portalName, String objectId) {
    return SLASH + PortalContainer.getInstance().getRestContextName() + "/ks/" + appType + "/rss/" + objectId;
  }

  public static String getUserRSSLink(String apptype, String userId) {
    return SLASH + PortalContainer.getInstance().getRestContextName() + "/ks/" + apptype + "/rss/user/" + userId;
  }
  
  /**
   * Check string is null or empty 
   * @param String s
   * @return boolean
   */
  public static boolean isEmpty(String s) {
    return (s == null || s.trim().length() <= 0) ? true : false;
  }

  /**
   * check string array is whether empty or not
   * @param array
   * @return false if at least one element of array is not empty, true in the opposite case.
   */
  public static boolean isEmpty(String[] array) {
    if (array != null && array.length > 0) {
      for (String s : array) {
        if (s != null && s.trim().length() > 0)
          return false;
      }
    }
    return true;
  }
  
  /**
   * Filter all invalid character (anything except word, number, space and search wildcard) from search conditional.
   * @param input the input string
   * @return String after remove all special characters
   * @since 4.0.x
   */
  public static String removeSpecialCharacterForSearch(String input) {
    if (isEmpty(input)) {
      return input;
    }
    String result = input.replaceAll(SPECIAL_CHARACTOR_FORSERACH_REGEX, " ");
    result = result.replaceAll("\\s+", " ");
    return result.trim();
  }
  
  public static String removeSpecialCharacterForUnifiedSearch(String input) {
    if (isEmpty(input)) {
      return input;
    }
    StringBuilder builder = new StringBuilder();
    String[] tab = input.split(" ");
    for (String s : tab){
      if (isEmpty(s)) continue;
      String searchTerm = s.split("~")[0];
      searchTerm = encodeSpecialCharToHTMLnumber(searchTerm.replaceAll(SPECIAL_CHARACTOR_FOR_UNIFIED_SERACH_REGEX, ""), "~", true);
      builder.append(searchTerm).append(" ");
    }
    return builder.toString().trim();
  }
  
  public static String processUnifiedSearchSearchCondition(String input) {
    if (isEmpty(input) || input.indexOf("~") < 0 || input.indexOf("\\~") > 0) {
      return input;
    }
    StringBuilder builder = new StringBuilder();
    String[] tab = input.split(" ");
    for (String s : tab){
      if (isEmpty(s)) continue;
      if (s.indexOf("~") > -1) {
        String searchTerm = s.split("~")[0];
        String similarity = s.split("~")[1];
        searchTerm = encodeSpecialCharToHTMLnumber(searchTerm.replaceAll(SPECIAL_CHARACTOR_FOR_UNIFIED_SERACH_REGEX, ""), "~", true);
        builder.append(searchTerm).append("~").append(similarity).append(" ");
      } else {
        String searchTerm = encodeSpecialCharToHTMLnumber(s.replaceAll(SPECIAL_CHARACTOR_FOR_UNIFIED_SERACH_REGEX, ""), "~", true);
        builder.append(searchTerm).append(" ");
      }
    }
    return builder.toString().trim();
  }
  
  /**
   * Normalize Unified search input
   * 
   * @param input The key search
   * @return 
   */
  public static String normalizeUnifiedSearchInput(String input) {
    if (isEmpty(input)) {
      return input;
    }
    StringBuilder builder = new StringBuilder();
    String keySearch = removeExceptPattern(input);
    String wildcardCharacters = (has2BytesCharacter(keySearch)) ? PERCENT_STR : ASTERISK_STR;
    StringTokenizer tokenizer = new StringTokenizer(keySearch, SPACE);
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      token = encodeSpecialCharToHTMLnumber(token.replaceAll(SPECIAL_CHARACTOR_FOR_UNIFIED_SERACH_REGEX, EMPTY_STR), "~", true);
      if (token.length() > 0) {
        builder.append(wildcardCharacters).append(token).append(wildcardCharacters).append((tokenizer.hasMoreTokens()) ? SPACE : EMPTY_STR);
      }
    }
    return builder.toString();
  }

  /**
   * Remove the except pattern
   * 
   * @param input
   * @return
   */
  private static String removeExceptPattern(String input) {
    return EXCEPT_PATTERN.matcher(input.trim()).replaceAll(EMPTY_STR);
  }
  /**
   * Check the input text has 2 bytes character or 1 byte
   * @param input
   * @return
   */
  private static boolean has2BytesCharacter(String input) {
    for (int i = 0; i < input.length(); i++) {
      int c = input.charAt(i);
      if (c > 255) {
        return true;
      }
    }
    return false;
  }

  /**
   * To check the input content has special characters or not.
   * @param input
   * @return
   */
  public static boolean hasSpecialCharacter(String input){
    if (isEmpty(input)) {
      return false;
    }
    String result = input.replaceAll(SPECIAL_CHARACTOR_REGEX, "").replaceAll("_", "");
    return result.equals(input) == false;
  }

  /**
   * Process the search condition correspond to each context
   * @param searchCondition the search condition
   */
  public static String processSearchCondition(String searchCondition) {
    if (isEmpty(searchCondition)) {
      return searchCondition;
    }
    StringBuilder searchConditionBuffer = new StringBuilder();
    //process the special characters
    searchCondition = removeSpecialCharacterForSearch(searchCondition);
    searchCondition = searchCondition.replace(ASTERISK_STR, PERCENT_STR);
    searchConditionBuffer.append(PERCENT_STR).append(searchCondition).append(PERCENT_STR);
    
    return searchConditionBuffer.toString();
  }
  
  /**
   * Get excerpt of given string with start position is the first position of textQuery.
   * @param str 
   *            the give string
   * @param textQuery
   *            the text that we truncate at the first appear
   * @param maxLength
   *            max of string length
   * @return the excerpt string
   */
  public static String getExcerpt(String str, String textQuery, int maxLength) {
    //
    if(str.length() <= maxLength) return str;
    
    //look for highlight to truncate 
    int position = str.indexOf("<strong>");
    //if not found
    if(position < 0) {
      position = Math.max(0, str.indexOf(textQuery));
    }
    return centerTrunc(str, position, maxLength);
  }
  
  /**
   * Truncates large Strings showing the string around the specific middle position with the specific length.
   * 
   * @param str
   *            the string to truncate
   * @param middlePosition
   *            the middle position we will show string around 
   * @param maxLength
   *            the max length of string to show
   * @return the truncated string
   */
  public static final String centerTrunc( String str, int middlePosition, int maxLength) {
      StringBuffer buf = null;
      //
      if ( str.length() <= maxLength )
      {
          return str;
      }
      
      int halfLength = maxLength / 2;
      //start position
      int start = 0;
      
      if( middlePosition > halfLength ) {
        start = ( middlePosition - halfLength );
      }
      
      //end position
      int end = Math.min( str.length(), start + maxLength );

      buf = new StringBuffer();
      buf.append(str.substring( start, end ));
      
      //complete first & last words
      String ret = buf.substring(buf.indexOf(" "));
      //
      if(start > 0)  ret = "..." + ret;
      
      ret = ret.substring(0, ret.lastIndexOf(" ")) + " ...";
      
      return ret;
  }
  
  /**
   * Encode special character, use for input search
   * @param String s, the string input
   * @return String 
   */
  public static String encodeSpecialCharInSearchTerm(String s) {
    /*
     * + When all characters in param s is special characters has in charIgnore, we must encode all characters.
     * + If all characters in param s is not special characters, we can ignore some special characters [!#:?=.,+;~`_]
    */
    String charIgnore = "&#<>[]/:?\"'=.,*$%()\\+@!^*-}{;`~_";
    if (!isEmpty(s)) {
      int i = 0;
      while (charIgnore.indexOf(String.valueOf(s.charAt(i))) >= 0) {
        ++i;
        if (i == s.length()) {
          charIgnore = EMPTY_STR;
          break;
        }
      }
    }
    if (!isEmpty(charIgnore)) charIgnore = "&</>!#:?=.,+;~`_";
    return encodeSpecialCharToHTMLnumber(s, charIgnore, true);
  }

  /**
   * Encode special character, use for input title or name of the object.
   * @param String s, the string input
   * @return String 
   */
  public static String encodeSpecialCharInTitle(String s) {
    /*
     * remove double space 
    */
    if(!isEmpty(s)) {
      while (s.indexOf("  ") >= 0) {
        s = StringUtils.replace(s, "  ", SPACE).trim();
      }
    }
    /*
     * charIgnore: Some special characters we ignore
    */
    String charIgnore = "!#:?=.,+;~`_";
    return encodeSpecialCharToHTMLnumber(s, charIgnore, true);
  }

  /**
   * Encode special character, use for input content of object (only apply for input by FCKEditer).
   * @param String s, the string input
   * @return String 
   */
  public static String encodeSpecialCharInContent(String s) {
    /*
     * charIgnore: Some special characters we ignore
    */
    String charIgnore = "&#<>[]/:?\"=.,*$%()\\+@!^*-}{;`~_";
    return encodeSpecialCharToHTMLnumber(s, charIgnore, false);
  }
  
  public static boolean isContainSpecialCharacter(String s) {
    if (isEmpty(s)) {
      return false;
    }
    String charIgnore = "&#<>[]/:?\"=.,*$%()\\+@!^*-}{;`~_";
    int i = 0;
    while (i < s.length()) {
      char c = s.charAt(i);
      if (charIgnore.indexOf(String.valueOf(c)) >= 0) {
        return true;
      }
      i++;
    }
    return false;
  }

  /**
   * Encode special character to html number. Ex: '/' --> &#47; 
   * @param String s, the string input
   * @param String charIgnore, the string content ignore some special character can not encode.
   * @param boolean isTitle, the boolean for check convert is title or not.
   * @return String 
   */
  public static String encodeSpecialCharToHTMLnumber(String s, String charIgnore, boolean isTitle) {
    if (isEmpty(s)) {
      return EMPTY_STR;
    }
    int i = 0;
    StringBuilder builder = new StringBuilder();
    while (i < s.length()) {
      char c = s.charAt(i);
      if (charIgnore.indexOf(String.valueOf(c)) >= 0) {
        builder.append(c);
      } else {
        int t = s.codePointAt(i);
        if (t < CHAR_CODES[0] && t > CHAR_CODES[1] || t < CHAR_CODES[2] && t > CHAR_CODES[3] ||
            t < CHAR_CODES[4] && t > CHAR_CODES[5] || t < CHAR_CODES[6] && t > CHAR_CODES[7]) {
          if (isTitle && (t == 60 || t == 62)) {
            if (t == 60) {
              builder.append(LESS_THAN);
            } else if (t == 62) {
              builder.append(GREATER_THAN);
            }
          } else {
            builder.append(AMP_NUMBER).append(t).append(SEMICOLON);
          }
        } else {
          builder.append(c);
        }
      }
      ++i;
    }
    return builder.toString();
  }

  public static String decodeSpecialCharToHTMLnumber(String s, List<String> lIgnore) {
    if (isEmpty(s)){
      return s;
    }
    for (String token : tokens) {
      if (lIgnore.contains(token)){
        continue;
      }
      while (token != null && s.indexOf(token) >= 0) {
        s = StringUtils.replace(s, token, charcodes.get(token));
      }
    }
    return s;
  }
  
  static {
    if(tokens.isEmpty()) {
      String token;
      // Tokens by HTML(Decimal) code.
      for (int t = Character.MIN_CODE_POINT; t < Character.MAX_CODE_POINT; t++) {
        if (t < CHAR_CODES[0] && t > CHAR_CODES[1] || t < CHAR_CODES[2] && t > CHAR_CODES[3] || 
            t < CHAR_CODES[4] && t > CHAR_CODES[5] || t < CHAR_CODES[6] && t > CHAR_CODES[7]) {
          token = new StringBuilder(AMP_NUMBER).append(t).append(SEMICOLON).toString();
          tokens.add(token);
          charcodes.put(token, String.valueOf(Character.toChars(t)[0]));
        }
      }
      // Tokens by Entity code.
      tokens.add(LESS_THAN);
      charcodes.put(LESS_THAN, "<");
      tokens.add(GREATER_THAN);
      charcodes.put(GREATER_THAN, ">");
      tokens.add(QUOT);
      charcodes.put(QUOT, "\"");
      tokens.add(AMP_SPACE);
      charcodes.put(AMP_SPACE, SPACE);
      tokens.add(AMP_HEX);
      charcodes.put(AMP_HEX, "&");
      tokens.add(AMP);
      charcodes.put(AMP, "&");
    }
  }

  public static String decodeSpecialCharToHTMLnumber(String s) {
    return decodeSpecialCharToHTMLnumber(s, new ArrayList<String>());
  }
  
  /**
   *  Decode special chars to HTML number ignore char Less than '<' and Greater than '>' 
   * 
   * @param str
   * @return
   */
  public static String decodeSpecialCharToHTMLnumberIgnore(String str) {
    return decodeSpecialCharToHTMLnumber(str, ignoreLessThanAndGreaterThan);
  }

  /**
   * Get current time GMT/Zulu or UTC,(zone time is 0+GMT)
   * @return Calendar 
   */
  static public Calendar getGreenwichMeanTime() {
    Calendar calendar = GregorianCalendar.getInstance();
    calendar.setLenient(false);
    int gmtoffset = calendar.get(Calendar.DST_OFFSET) + calendar.get(Calendar.ZONE_OFFSET);
    calendar.setTimeInMillis(System.currentTimeMillis() - gmtoffset);
    return calendar;
  }
  
  public static <T>T getComponent(Class<T> type) {
    return type.cast(PortalContainer.getInstance().getComponentInstanceOfType(type));
  }

  public static SessionProvider createSystemProvider() {
    SessionProviderService sessionProviderService = getComponent(SessionProviderService.class);
    return sessionProviderService.getSystemSessionProvider(null);
  }
  
}
