# -*- coding: utf-8 -*-

class ServiceNotLocatedException(Exception):
    def __init__(self, method):
        self.method = method

    def __str__(self):
        return repr(self.method)



class ServiceLocator(object):
    """An abstract interface to locate service URI from method.
    This class isn't needed in fact since Python is duck-typing.
    """
    __module__ = __name__

    def locate(self, method):
        'Returns a URI from method name'
        pass


class RegexLocator(object):

    def __init__(self, mapper):
        from re import compile as rc 
        self.mapper = tuple([(rc(r), u) for (r, u) in mapper.iteritems()])

    def locate(self, method):
        for (r, u,) in self.mapper:
            if r.search(method):
                return u
        raise ServiceNotLocatedException(method)

class WildcardLocator(object):
    __module__ = __name__

    def __init__(self, mapper):
        self.mapper = tuple(mapper.iteritems())

    def locate(self, method):
        from fnmatch import fnmatch as fm 
        for (w, u,) in self.mapper:
            if fm(method, w):
                return u
        raise ServiceNotLocatedException(method)

