# -*- coding: utf-8 -*-

from mustaine.client import HessianProxy 
from mustaine.protocol import Object 

class AlbusError(Exception):
    def __init__(self, code, message, stack):
        self.code = code
        self.message = message
        self.stack = stack
    def __str__(self):
        msg = u'< code:%s, message:"%s", stack:"%s" >' % (
            self.code,
            self.message,
            self.stack)
        return msg.encode('utf-8')

def albus_method(tx, version = '1.0'):
    """This decorator indicates that an instance-method
    of a class is an Albus method.

    `tx` : name of the remote method. may be different to local name.
    `version` : desired protocol version of remote interface.
    """

    def deco(fun):
        def real_fun(self, *args, **kwargs):
            uri = self.locator.locate(tx)
            service = HessianProxy(uri, timeout=self.timeout)
            res = service.invoke(Object('net.butfly.bus.Request', tx=tx, version=version, args=args, extParamMap=kwargs))
            if res.errorCode is not None:
                raise AlbusError(res.errorCode, res.errorMessage, res.errorStack)
            return res.resultCode, res.result
        return real_fun
    return deco


class AlbusClient(object):
    'Albus Client'
    __module__ = __name__

    def __init__(self, locator, timeout=60):
        self.locator = locator
        self.timeout = timeout

