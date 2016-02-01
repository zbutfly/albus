# -*- coding: utf-8 -*-

from albus.client import AlbusClient
from albus.client import AlbusError
from albus.client import albus_method
from albus.locator import WildcardLocator
from albus.locator import RegexLocator

class DemoBusClient(AlbusClient):
    @albus_method('SPL_001')
    def echo(self, text):
        pass

    @albus_method('NOR_001')
    def noroute(self, _):
        pass

    @albus_method('SPL_002')
    def nomethod(self, _):
        pass

cli = DemoBusClient(WildcardLocator(
    { 'SPL_*': 'http://10.202.73.220:8080/bus/z' }
))

for method in ['echo', 'noroute', 'nomethod']:
    try:
        rc, res = getattr(cli, method)('hello')
        print 'OK "%s" : %s - %s' % (method, rc, res)
    except Exception as e:
        print 'ERR "%s" : %s' % (method, str(e))
