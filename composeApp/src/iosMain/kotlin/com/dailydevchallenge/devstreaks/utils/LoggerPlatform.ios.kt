package com.dailydevchallenge.devstreaks.utils

import com.dailydevchallenge.devstreaks.logger.DevLogger
import com.dailydevchallenge.devstreaks.logger.IOSDevLogger

actual fun getLogger(): DevLogger = IOSDevLogger()