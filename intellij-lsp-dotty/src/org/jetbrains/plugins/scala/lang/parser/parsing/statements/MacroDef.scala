package org.jetbrains.plugins.scala.lang.parser.parsing.statements

import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder
import org.jetbrains.plugins.scala.lang.parser.parsing.expressions.Block
import org.jetbrains.plugins.scala.lang.parser.parsing.top.Qual_Id
import org.jetbrains.plugins.scala.lang.parser.parsing.types.{Type, TypeArgs}

/**
 * @author Jason Zaugg
 *
 * MacroDef ::= MacroDef ::= FunSig [‘:’ Type] ‘=’ ‘macro’ QualId [TypeArgs]
 */
object MacroDef extends MacroDef {
  override protected def funSig = FunSig
  override protected def `type` = Type
  override protected def typeArgs = TypeArgs
}

trait MacroDef {
  protected def funSig: FunSig
  protected def `type`: Type
  protected def typeArgs: TypeArgs

  def parse(builder: ScalaPsiBuilder): Boolean = {
    val marker = builder.mark
    builder.getTokenType match {
      case ScalaTokenTypes.kDEF => builder.advanceLexer()
      case _ =>
        marker.drop()
        return false
    }
    builder.getTokenType match {
      case ScalaTokenTypes.tIDENTIFIER =>
        funSig parse builder
        builder.getTokenType match {
          case ScalaTokenTypes.tCOLON =>
            builder.advanceLexer() //Ate :
            if (`type`.parse(builder)) {
              builder.getTokenType match {
                case ScalaTokenTypes.tASSIGN =>
                  builder.advanceLexer() //Ate =
                  builder.getTokenType match {
                    case ScalaTokenTypes.kMACRO =>
                      builder.advanceLexer() //Ate `macro`
                      builder.getTokenType match {
                        case ScalaTokenTypes.tLBRACE => // scalameta style - embedded macro body
                          if (builder.twoNewlinesBeforeCurrentToken) {
                            return false
                          }
                          Block.parse(builder, hasBrace = true)
                          marker.drop()
                          true
                        case _ =>
                          if (Qual_Id.parse(builder)) {
                            if (builder.getTokenType == ScalaTokenTypes.tLSQBRACKET) {
                              TypeArgs.parse(builder, isPattern = false)
                            }
                            marker.drop()
                            true
                          } else {
                            marker.drop()
                            false
                          }
                      }
                    case _ =>
                      marker.rollbackTo()
                      false
                  }
                case _ =>
                  marker.rollbackTo()
                  false
              }
            }
            else {
              marker.rollbackTo()
              false
            }
          case ScalaTokenTypes.tASSIGN =>
            builder.advanceLexer() //Ate =
            builder.getTokenType match {
              case ScalaTokenTypes.kMACRO =>
                builder.advanceLexer() //Ate `macro`
                builder.getTokenType match {
                  case ScalaTokenTypes.tLBRACE =>  // scalameta style - embedded macro body
                    if (builder.twoNewlinesBeforeCurrentToken) {
                      return false
                    }
                    Block.parse(builder, hasBrace = true)
                    marker.drop()
                    true
                  case _ =>
                    if (Qual_Id.parse(builder)) {
                      if (builder.getTokenType == ScalaTokenTypes.tLSQBRACKET) {
                        TypeArgs.parse(builder, isPattern = false)
                      }
                      marker.drop()
                      true
                    } else {
                      marker.drop()
                      false
                    }
                }
              case _ =>
                marker.rollbackTo()
                false
            }
          case _ =>
            marker.rollbackTo()
            false
        }
      case _ =>
        marker.rollbackTo()
        false
    }
  }
}
