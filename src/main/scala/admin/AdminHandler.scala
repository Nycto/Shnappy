package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._

/**
 * Admin handlers
 */
class AdminHandler extends Skene {
    delegate( new PageHandler )
}


