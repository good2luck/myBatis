/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

/**
 * 一般边界解析类
 * @author Clinton Begin
 */
public class GenericTokenParser {

  private final String openToken;	//开始串
  private final String closeToken;	//结束串
  private final TokenHandler handler;	//TokenHandler处理器

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;		
    this.closeToken = closeToken;
    this.handler = handler;
  }

  /*
   * 解析字符串（node节点中的属性值）
   * 例如：如果引用的外部配置信息中包括${str}中的引用字符串，则str改成外部真正配置值；外部没找到的话返回${str}
   * 下面以${}为例说明，#{}与${}相同
   */
  public String parse(String text) {
    final StringBuilder builder = new StringBuilder();
    final StringBuilder expression = new StringBuilder();//存储${str}中的str串
    if (text != null && text.length() > 0) {	//传入的串有值
      char[] src = text.toCharArray();
      int offset = 0;
      // search open token
      int start = text.indexOf(openToken, offset);	//从下边offset开始查询第一次出现openToken(例如"${"、"#{")的位置
      while (start > -1) {//一直有的话（一组${}循环一次）
        if (start > 0 && src[start - 1] == '\\') {//${不是text的开始且${前面为\\时，移除\\
          // this open token is escaped. remove the backslash and continue.
          builder.append(src, offset, start - offset - 1).append(openToken);
          offset = start + openToken.length();
        } else {//找到${后，开始找}
          // found open token. let's search close token.
          expression.setLength(0);
          builder.append(src, offset, start - offset);
          offset = start + openToken.length();
          int end = text.indexOf(closeToken, offset);
          while (end > -1) {
            if (end > offset && src[end - 1] == '\\') {//}不是text的结束时且}前面为\\时，移除\\
              // this close token is escaped. remove the backslash and continue.
              expression.append(src, offset, end - offset - 1).append(closeToken);
              offset = end + closeToken.length();
              end = text.indexOf(closeToken, offset);
            } else {
              expression.append(src, offset, end - offset);
              offset = end + closeToken.length();
              break;
            }
          }
          if (end == -1) {//未找到}
            // close token was not found.
            builder.append(src, start, src.length - start);
            offset = src.length;
          } else {
            builder.append(handler.handleToken(expression.toString()));
            offset = end + closeToken.length();
          }
        }
        start = text.indexOf(openToken, offset);
      }
      if (offset < src.length) {//有}时,}后面的追加到字符串后面 
        builder.append(src, offset, src.length - offset);
      }
    }
    return builder.toString();
  }
}
