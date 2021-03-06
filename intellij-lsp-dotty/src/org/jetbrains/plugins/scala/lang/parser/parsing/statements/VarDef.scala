package org.jetbrains.plugins.scala.lang.parser.parsing.statements

import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.parsing.base.Ids
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder
import org.jetbrains.plugins.scala.lang.parser.parsing.types.Type
import org.jetbrains.plugins.scala.lang.parser.util.ParserUtils

/**
* @author Alexander Podkhalyuzin
* Date: 06.02.2008
*/

/*
 *  ValDef ::= PatDef |
 *             ids ':' Type '=' '_'
 */
object VarDef extends VarDef {
  override protected def patDef = PatDef
  override protected def `type` = Type
}

trait VarDef {
  protected def patDef: PatDef
  protected def `type`: Type

  def parse(builder: ScalaPsiBuilder): Boolean = {
    if (patDef parse builder) {
      return true
    }

    // Parsing specifig wildcard definition
    val valDefMarker = builder.mark
    builder.getTokenType match {
      case ScalaTokenTypes.tIDENTIFIER =>
        Ids parse builder
        var hasTypeDcl = false

        if (ScalaTokenTypes.tCOLON.equals(builder.getTokenType)) {
          ParserUtils.eatElement(builder, ScalaTokenTypes.tCOLON)
          if (!`type`.parse(builder)) {
            builder error "type declaration expected"
          }
          hasTypeDcl = true
        }
        else {
          valDefMarker.rollbackTo
          return false
        }
        if (!ScalaTokenTypes.tASSIGN.equals(builder.getTokenType)) {
          valDefMarker.rollbackTo
          return false
        } else {
          ParserUtils.eatElement(builder, ScalaTokenTypes.tASSIGN)
          builder.getTokenType match {
            case ScalaTokenTypes.tUNDER => builder.advanceLexer
            //Ate _
            case _ => {
              valDefMarker.rollbackTo
              return false
            }
          }
          valDefMarker.drop
          return true
        }
      case _ =>
        valDefMarker.drop
        return false
    }
  }
}